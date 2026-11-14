package br.edu.ifrn.promptuario.backup.dumper;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.model.BackupKind;
import br.edu.ifrn.promptuario.backup.model.DatabaseBackupResult;
import br.edu.ifrn.promptuario.backup.model.DatabaseEndpoint;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.model.ExecutionMode;
import br.edu.ifrn.promptuario.backup.service.CryptoService;
import br.edu.ifrn.promptuario.backup.storage.S3BackupStorageService;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;
import javax.crypto.CipherOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base dos dumpers que executam uma ferramenta externa e gravam o artefato em
 * streaming, sem carregar o dump em memória.
 *
 * <p>Pipeline obrigatório (nunca materializa o dump em RAM):
 * <pre>
 *   ProcessBuilder(stdout) ─► GZIPOutputStream ─► CipherOutputStream ─► DigestOutputStream ─► FileOutputStream
 *                                                        (AES-256-GCM)        (SHA-256)
 * </pre>
 *
 * <p>O {@code GZIPOutputStream} é aplicado apenas quando a ferramenta não
 * comprime por conta própria ({@link #compressionEnabled()}) — o
 * {@code mongodump} já é invocado com {@code --gzip}, e recomprimir corromperia
 * o formato esperado pelo {@code mongorestore}.
 *
 * <p>O SHA-256 é calculado sobre o <strong>arquivo final</strong> (incluindo o
 * prefixo do IV), garantindo a integridade do artefato exatamente como ele é
 * armazenado em disco e enviado ao MinIO.
 */
public abstract class AbstractStreamingDatabaseDumper implements DatabaseDumper {

    private static final Logger log = LoggerFactory.getLogger(AbstractStreamingDatabaseDumper.class);

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);
    private static final int STDERR_CAPTURE_LIMIT = 4096;
    private static final int PUMP_BUFFER_SIZE = 64 * 1024;
    private static final long STDERR_JOIN_TIMEOUT_MS = 5_000L;
    private static final long POST_EOF_WAIT_SECONDS = 30L;

    protected final BackupProperties backupProperties;
    protected final CryptoService cryptoService;
    protected final S3BackupStorageService storageService;

    protected AbstractStreamingDatabaseDumper(
            BackupProperties backupProperties,
            CryptoService cryptoService,
            S3BackupStorageService storageService) {
        this.backupProperties = backupProperties;
        this.cryptoService = cryptoService;
        this.storageService = storageService;
    }

    // ── Hooks das implementações ─────────────────────────────────────────────

    /** Natureza do artefato gerado por este dumper. */
    protected abstract BackupKind kind(DatabaseTarget target);

    /** Resolve host/porta/credenciais do alvo (lança se o alvo não for suportado). */
    protected abstract DatabaseEndpoint endpoint(DatabaseTarget target);

    /** Monta a linha de comando da ferramenta de dump. */
    protected abstract List<String> buildCommand(DatabaseTarget target, DatabaseEndpoint endpoint);

    /** Variáveis de ambiente extras (ex.: {@code PGPASSWORD}). */
    protected abstract Map<String, String> environment(DatabaseEndpoint endpoint);

    /** Prefixo do nome do arquivo ({@code postgres} / {@code mongo}). */
    protected abstract String artifactPrefix();

    /** Extensão do artefato ({@code .sql.gz} / {@code .archive.gz}). */
    protected abstract String artifactExtension();

    /** {@code true} quando o pipeline deve aplicar gzip antes de cifrar. */
    protected abstract boolean compressionEnabled();

    // ── Template principal ───────────────────────────────────────────────────

    @Override
    public DatabaseBackupResult dump(DatabaseTarget target, ExecutionMode mode) {
        BackupKind kind = kind(target);
        Path artifact = null;
        long startNanos = System.nanoTime();

        try {
            DatabaseEndpoint endpoint = endpoint(target);
            artifact = resolveArtifactPath(kind, endpoint.dbname());
            Files.createDirectories(artifact.getParent());

            log.info("Iniciando dump de {} ({}) no modo {} -> {}", target, endpoint.dbname(), mode, artifact);
            StreamOutcome outcome = runPipeline(target, endpoint, artifact);

            String sha256 = outcome.sha256Hex();
            double durationSeconds = elapsedSeconds(startNanos);
            String objectKey =
                    storageService.buildObjectKey(kind, endpoint.dbname(), artifact.getFileName().toString());

            try {
                storageService.uploadArtifact(
                        artifact, objectKey, buildMetadata(target, sha256, outcome.sizeBytes()));
            } catch (RuntimeException uploadError) {
                String message = "Upload do artefato para o MinIO falhou (arquivo local preservado em "
                        + artifact + "): " + rootMessage(uploadError);
                log.error("{}", message);
                return DatabaseBackupResult.failure(target, kind, message);
            }

            log.info("Dump de {} concluído: {} bytes, sha256={}, {}s",
                    target, outcome.sizeBytes(), sha256, durationSeconds);
            return DatabaseBackupResult.success(
                    target, kind, artifact.toString(), objectKey, outcome.sizeBytes(), durationSeconds, sha256);

        } catch (DumpExecutionException | IOException e) {
            deleteQuietly(artifact);
            String message = "Backup de " + target + " falhou: " + rootMessage(e);
            log.error("{}", message);
            return DatabaseBackupResult.failure(target, kind, message);
        } catch (RuntimeException e) {
            deleteQuietly(artifact);
            String message = "Erro inesperado no backup de " + target + ": " + rootMessage(e);
            log.error(message, e);
            return DatabaseBackupResult.failure(target, kind, message);
        }
    }

    // ── Execução do pipeline ─────────────────────────────────────────────────

    private StreamOutcome runPipeline(DatabaseTarget target, DatabaseEndpoint endpoint, Path artifact)
            throws IOException, DumpExecutionException {

        List<String> command = buildCommand(target, endpoint);
        Map<String, String> extraEnvironment = environment(endpoint);
        Duration timeout = Duration.ofSeconds(backupProperties.dumpTimeoutSeconds());
        MessageDigest sha256 = sha256Digest();

        log.debug("Executando dump de {} (timeout de {}s)", target, timeout.toSeconds());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().putAll(extraEnvironment);
        processBuilder.redirectErrorStream(false);
        Process process = processBuilder.start();
        process.getOutputStream().close(); // o dump não consome stdin

        // stderr é drenado em thread dedicada: sem isso o processo pode bloquear
        // quando o buffer do pipe enche (deadlock clássico de ProcessBuilder).
        ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();
        Thread stderrThread = Thread.ofVirtual()
                .name("dump-stderr-" + endpoint.dbname())
                .start(() -> drainStderr(process.getErrorStream(), stderrBuffer));

        AtomicBoolean timedOut = new AtomicBoolean(false);
        Thread watchdog = startTimeoutWatchdog(process, timeout, timedOut);

        try {
            try (FileOutputStream fileOut = new FileOutputStream(artifact.toFile());
                 DigestOutputStream digestOut = new DigestOutputStream(fileOut, sha256)) {

                CipherOutputStream cipherOut = null;
                GZIPOutputStream gzipOut = null;
                try {
                    OutputStream sink = digestOut;
                    if (cryptoService.isEnabled()) {
                        cipherOut = cryptoService.createEncryptingStream(digestOut, cryptoService.activeKey());
                        sink = cipherOut;
                    }
                    if (compressionEnabled()) {
                        gzipOut = new GZIPOutputStream(sink, true);
                        sink = gzipOut;
                    }
                    try (InputStream processStdout = process.getInputStream()) {
                        pump(processStdout, sink);
                    }
                } finally {
                    // Fechamento na ordem inversa do pipeline: gzip → cipher
                    // (o close do CipherOutputStream grava a tag GCM de autenticação).
                    closeQuietly(gzipOut);
                    closeQuietly(cipherOut);
                }
            }
        } finally {
            watchdog.interrupt();
        }

        joinStderr(stderrThread);

        if (timedOut.get()) {
            destroyQuietly(process);
            throw new DumpExecutionException(
                    "Timeout de " + timeout.toSeconds() + "s excedido; processo de dump encerrado à força");
        }
        boolean exited;
        try {
            exited = process.waitFor(POST_EOF_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            destroyQuietly(process);
            throw new DumpExecutionException("Thread interrompida durante o dump de " + target);
        }
        if (!exited) {
            destroyQuietly(process);
            throw new DumpExecutionException("Processo de dump não encerrou após o fim do stream de saída");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new DumpExecutionException(
                    "comando retornou código " + exitCode + ": " + stderrText(stderrBuffer));
        }

        long sizeBytes = Files.size(artifact);
        String sha256Hex = HexFormat.of().formatHex(sha256.digest());
        return new StreamOutcome(sizeBytes, sha256Hex);
    }

    /**
     * Vigia o processo e o encerra à força se o timeout for excedido. É o que
     * permite aplicar o timeout mesmo quando o dump "trava" no meio da leitura
     * (situação em que um {@code waitFor} após o EOF nunca seria alcançado).
     */
    private Thread startTimeoutWatchdog(Process process, Duration timeout, AtomicBoolean timedOut) {
        return Thread.ofVirtual().name("dump-watchdog").start(() -> {
            try {
                if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
                    timedOut.set(true);
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /** Copia o stream do processo para o pipeline usando um único buffer de 64 KB. */
    private static void pump(InputStream source, OutputStream sink) throws IOException {
        byte[] buffer = new byte[PUMP_BUFFER_SIZE];
        int read;
        while ((read = source.read(buffer)) != -1) {
            sink.write(buffer, 0, read);
        }
        sink.flush();
    }

    /** Drena o stderr em thread dedicada, com captura limitada para não crescer sem controle. */
    private static void drainStderr(InputStream errorStream, ByteArrayOutputStream buffer) {
        try (InputStream in = errorStream) {
            byte[] chunk = new byte[1024];
            int read;
            while ((read = in.read(chunk)) != -1) {
                int remaining = STDERR_CAPTURE_LIMIT - buffer.size();
                if (remaining > 0) {
                    buffer.write(chunk, 0, Math.min(read, remaining));
                }
            }
        } catch (IOException e) {
            log.debug("Stream de stderr encerrado: {}", e.getMessage());
        }
    }

    private static void joinStderr(Thread stderrThread) {
        try {
            stderrThread.join(STDERR_JOIN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    /**
     * Resolve o caminho local do artefato, espelhando a estrutura de chave do
     * MinIO: {@code {rootDir}/{kind}/{dbname}/{yyyy}/{MM}/{dd}/{filename}}.
     */
    private Path resolveArtifactPath(BackupKind kind, String dbname) {
        Instant now = Instant.now();
        LocalDate date = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Path directory = backupProperties.rootDir()
                .resolve(kind.folder())
                .resolve(dbname)
                .resolve("%04d".formatted(date.getYear()))
                .resolve("%02d".formatted(date.getMonthValue()))
                .resolve("%02d".formatted(date.getDayOfMonth()));
        String filename = artifactPrefix() + "_" + dbname + "_" + TIMESTAMP.format(now)
                + artifactExtension() + (cryptoService.isEnabled() ? ".enc" : "");
        return directory.resolve(filename);
    }

    /** Metadados gravados no objeto do MinIO (integridade e rastreabilidade). */
    private Map<String, String> buildMetadata(DatabaseTarget target, String sha256Hex, long sizeBytes) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sha256", sha256Hex);
        metadata.put("target", target.name());
        metadata.put("timestamp", TIMESTAMP.format(Instant.now()));
        metadata.put("size-bytes", Long.toString(sizeBytes));
        return metadata;
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static double elapsedSeconds(long startNanos) {
        return Math.round(((System.nanoTime() - startNanos) / 1_000_000_000.0d) * 100.0d) / 100.0d;
    }

    private static String stderrText(ByteArrayOutputStream buffer) {
        String text = buffer.toString(StandardCharsets.UTF_8).trim();
        return text.isEmpty() ? "(sem saída de erro)" : text;
    }

    /** Mensagem da causa raiz, garantindo texto não vazio para o manifesto. */
    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return (message == null || message.isBlank()) ? current.getClass().getSimpleName() : message;
    }

    private static void closeQuietly(OutputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
            log.warn("Falha ao fechar o stream do pipeline: {}", e.getMessage());
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            if (Files.deleteIfExists(path)) {
                log.warn("Artefato parcial removido: {}", path);
            }
        } catch (IOException e) {
            log.warn("Não foi possível remover o artefato parcial {}: {}", path, e.getMessage());
        }
    }

    private static void destroyQuietly(Process process) {
        try {
            process.destroyForcibly();
        } catch (RuntimeException e) {
            log.debug("Falha ao encerrar o processo de dump: {}", e.getMessage());
        }
    }

    private record StreamOutcome(long sizeBytes, String sha256Hex) {
    }
}
