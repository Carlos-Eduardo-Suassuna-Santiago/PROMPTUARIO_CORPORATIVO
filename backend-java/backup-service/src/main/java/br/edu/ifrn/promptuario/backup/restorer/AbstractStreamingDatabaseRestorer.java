package br.edu.ifrn.promptuario.backup.restorer;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.model.DatabaseEndpoint;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.service.CryptoService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import javax.crypto.CipherInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base dos restauradores que alimentam a ferramenta nativa pelo {@code stdin},
 * em streaming e sem materializar o dump em memória.
 *
 * <p>Pipeline de restauração:
 * <pre>
 *   FileInputStream ─► CipherInputStream (AES-256-GCM) ─► GZIPInputStream ─► ProcessBuilder stdin
 * </pre>
 *
 * <p>Três barreiras de segurança são aplicadas <em>antes</em> de qualquer
 * conexão ao banco:
 * <ol>
 *   <li>o arquivo precisa existir e ser legível;</li>
 *   <li>quando um SHA-256 é informado, ele é conferido contra o manifesto —
 *       divergência cancela a restauração sem abrir conexão;</li>
 *   <li>a tag de autenticação GCM invalida o stream imediatamente em caso de
 *       corrupção de bits ou adulteração.</li>
 * </ol>
 *
 * <p>Um watchdog impõe o timeout de segurança
 * ({@code promptuario.backup.restore-timeout-seconds}, padrão 30 min) mesmo se
 * a ferramenta travar no meio da leitura.
 */
public abstract class AbstractStreamingDatabaseRestorer implements DatabaseRestorer {

    private static final Logger log = LoggerFactory.getLogger(AbstractStreamingDatabaseRestorer.class);

    protected static final int PUMP_BUFFER_SIZE = 64 * 1024;
    private static final int OUTPUT_CAPTURE_LIMIT = 4096;
    private static final long DRAIN_JOIN_TIMEOUT_MS = 5_000L;
    private static final long POST_EOF_WAIT_SECONDS = 30L;
    private static final String REDACTED = "***";

    protected final BackupProperties backupProperties;
    protected final CryptoService cryptoService;

    protected AbstractStreamingDatabaseRestorer(BackupProperties backupProperties, CryptoService cryptoService) {
        this.backupProperties = backupProperties;
        this.cryptoService = cryptoService;
    }

    // ── Hooks das implementações ─────────────────────────────────────────────

    /** Resolve host/porta/credenciais do alvo (lança se o alvo não for suportado). */
    protected abstract DatabaseEndpoint endpoint(DatabaseTarget target);

    /** Monta a linha de comando da ferramenta de restauração. */
    protected abstract List<String> buildCommand(DatabaseEndpoint endpoint);

    /** Variáveis de ambiente extras (ex.: {@code PGPASSWORD}). */
    protected abstract Map<String, String> environment(DatabaseEndpoint endpoint);

    /** {@code true} quando o pipeline deve descomprimir após decifrar. */
    protected abstract boolean compressionEnabled();

    /** Nome da ferramenta, usado nas mensagens de erro. */
    protected abstract String toolName();

    @Override
    public boolean restore(DatabaseTarget target, Path encryptedBackupFile, String expectedSha256) {
        return restoreInto(endpoint(target), encryptedBackupFile, expectedSha256);
    }

    /**
     * Restaura para um endpoint explícito — permite que o drill de contingência
     * aponte para um banco efêmero sem tocar na produção.
     *
     * @throws RestoreFailedException se a restauração foi tentada e falhou
     */
    public boolean restoreInto(DatabaseEndpoint endpoint, Path encryptedBackupFile, String expectedSha256) {
        if (encryptedBackupFile == null || !Files.isRegularFile(encryptedBackupFile)) {
            log.error("Artefato de restauração inexistente ou ilegível: {}", encryptedBackupFile);
            return false;
        }

        try {
            if (!verifyChecksum(encryptedBackupFile, expectedSha256)) {
                return false; // barrado ANTES de qualquer conexão ao banco
            }
            runPipeline(endpoint, encryptedBackupFile);
            log.info("Restauração concluída em '{}' a partir de {}",
                    endpoint.dbname(), encryptedBackupFile.getFileName());
            return true;
        } catch (RestoreFailedException e) {
            log.error("Restauração de '{}' falhou: {}", endpoint.dbname(), e.getMessage());
            throw e;
        } catch (IOException e) {
            throw new RestoreFailedException(
                    "Falha de I/O na restauração de '" + endpoint.dbname() + "': " + e.getMessage(), e);
        }
    }

    // ── Execução do pipeline ─────────────────────────────────────────────────

    private void runPipeline(DatabaseEndpoint endpoint, Path encryptedBackupFile)
            throws IOException, RestoreFailedException {

        List<String> command = buildCommand(endpoint);
        Map<String, String> extraEnvironment = environment(endpoint);
        Duration timeout = Duration.ofSeconds(backupProperties.restoreTimeoutSeconds());

        log.info("Iniciando restauração de '{}' (timeout de segurança de {}s) a partir de {}",
                endpoint.dbname(), timeout.toSeconds(), encryptedBackupFile.getFileName());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().putAll(extraEnvironment);
        processBuilder.redirectErrorStream(false);
        Process process = processBuilder.start();

        // stdout/stderr drenados em threads dedicadas (evita deadlock de pipe).
        ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();
        Thread stdoutDrain = Thread.ofVirtual().name("restore-stdout-" + endpoint.dbname())
                .start(() -> drain(process.getInputStream(), stdoutBuffer));
        Thread stderrDrain = Thread.ofVirtual().name("restore-stderr-" + endpoint.dbname())
                .start(() -> drain(process.getErrorStream(), stderrBuffer));

        AtomicBoolean timedOut = new AtomicBoolean(false);
        Thread watchdog = startTimeoutWatchdog(process, timeout, timedOut);

        try {
            IOException pumpFailure = null;
            try {
                pumpIntoProcess(process, encryptedBackupFile);
            } catch (IOException e) {
                // Pode ser EPIPE porque a ferramenta já morreu (erro real no stderr)
                // ou porque o watchdog a encerrou. Avaliado logo abaixo.
                pumpFailure = e;
            }

            join(stdoutDrain);
            join(stderrDrain);

            verifyCompletion(process, endpoint, timeout, timedOut, pumpFailure, stderrBuffer, encryptedBackupFile);
        } finally {
            // O watchdog permanece armado durante toda a espera pelo processo;
            // interrompê-lo antes cancelaria o timeout de segurança.
            watchdog.interrupt();
        }
    }

    /** Valida o desfecho do processo e converte falhas em {@link RestoreFailedException}. */
    private void verifyCompletion(
            Process process,
            DatabaseEndpoint endpoint,
            Duration timeout,
            AtomicBoolean timedOut,
            IOException pumpFailure,
            ByteArrayOutputStream stderrBuffer,
            Path encryptedBackupFile) throws IOException {

        if (timedOut.get()) {
            destroyQuietly(process);
            throw new RestoreFailedException("Timeout de segurança de " + timeout.toSeconds()
                    + "s excedido na restauração de '" + endpoint.dbname() + "'");
        }

        boolean exited;
        try {
            exited = process.waitFor(POST_EOF_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            destroyQuietly(process);
            throw new RestoreFailedException(
                    "Thread interrompida durante a restauração de '" + endpoint.dbname() + "'");
        }
        if (!exited) {
            destroyQuietly(process);
            throw new RestoreFailedException(
                    toolName() + " não encerrou após o envio do stream para '" + endpoint.dbname() + "'");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            // A causa real está no stderr; a mensagem é saneada (sem credenciais).
            throw new RestoreFailedException(toolName() + " falhou (código " + exitCode + ") em '"
                    + endpoint.dbname() + "': " + sanitize(stderrText(stderrBuffer), endpoint));
        }
        if (pumpFailure != null) {
            // A ferramenta encerrou "bem", mas o dump não foi enviado integralmente
            // (ex.: tag GCM inválida no meio do stream) — restauração barrada.
            throw new RestoreFailedException(toolName() + " recebeu o dump incompleto em '" + endpoint.dbname()
                    + "': " + pumpFailure.getMessage(), pumpFailure);
        }

        log.debug("Stream de restauração de '{}' enviado ({} bytes no artefato).",
                endpoint.dbname(), Files.size(encryptedBackupFile));
    }

    /** Monta a cadeia decifra (e descomprime) e alimenta o stdin do processo. */
    private void pumpIntoProcess(Process process, Path encryptedBackupFile) throws IOException {
        try (InputStream fileIn = Files.newInputStream(encryptedBackupFile);
             OutputStream processStdin = process.getOutputStream()) {

            InputStream source = fileIn;
            CipherInputStream cipherIn = null;
            GZIPInputStream gzipIn = null;
            try {
                if (cryptoService.isEnabled()) {
                    cipherIn = cryptoService.createDecryptingStream(fileIn, cryptoService.activeKey());
                    source = cipherIn;
                }
                if (compressionEnabled()) {
                    gzipIn = new GZIPInputStream(source, PUMP_BUFFER_SIZE);
                    source = gzipIn;
                }
                pump(source, processStdin);
                if (cipherIn != null) {
                    forceCipherCompletion(cipherIn);
                }
            } finally {
                closeQuietly(gzipIn);
                closeQuietly(cipherIn);
            }
        }
    }

    /**
     * Drena o stream cifrado até o fim para forçar a verificação da tag de
     * autenticação GCM.
     *
     * <p>Sem isso, um descompressor que concluísse a leitura antes do fim do
     * stream cifrado encerraria o pipeline sem validar a tag — e o
     * {@code close()} do {@code CipherInputStream} engole {@code BadPaddingException}.
     * Este dreno garante que corrupção/adulteração seja <strong>sempre</strong>
     * detectada (princípio 2).
     */
    private static void forceCipherCompletion(InputStream cipherStream) throws IOException {
        byte[] scratch = new byte[1024];
        while (cipherStream.read(scratch) != -1) {
            // descarta: o conteúdo útil já foi enviado à ferramenta de restauração
        }
    }

    /**
     * Vigia o processo e o encerra à força se o timeout for excedido — sem isso
     * um stream travado nunca seria interrompido.
     */
    private Thread startTimeoutWatchdog(Process process, Duration timeout, AtomicBoolean timedOut) {
        return Thread.ofVirtual().name("restore-watchdog").start(() -> {
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

    /** Copia o stream para a ferramenta usando um único buffer de 64 KB. */
    private static void pump(InputStream source, OutputStream sink) throws IOException {
        byte[] buffer = new byte[PUMP_BUFFER_SIZE];
        int read;
        while ((read = source.read(buffer)) != -1) {
            sink.write(buffer, 0, read);
        }
        sink.flush();
    }

    private static void drain(InputStream stream, ByteArrayOutputStream buffer) {
        try (InputStream in = stream) {
            byte[] chunk = new byte[1024];
            int read;
            while ((read = in.read(chunk)) != -1) {
                int remaining = OUTPUT_CAPTURE_LIMIT - buffer.size();
                if (remaining > 0) {
                    buffer.write(chunk, 0, Math.min(read, remaining));
                }
            }
        } catch (IOException e) {
            log.debug("Stream do processo de restauração encerrado: {}", e.getMessage());
        }
    }

    private static void join(Thread thread) {
        try {
            thread.join(DRAIN_JOIN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Segurança e mensagens ────────────────────────────────────────────────

    /**
     * Princípio 1 — validação pré-restore: confere o SHA-256 do artefato contra
     * o manifesto <strong>antes</strong> de qualquer conexão ao banco.
     */
    private boolean verifyChecksum(Path file, String expectedSha256) throws IOException {
        if (expectedSha256 == null || expectedSha256.isBlank()) {
            log.warn("SHA-256 não informado para {} — validação pré-restore não executada "
                    + "(recomendado sempre informar o hash do manifesto).", file);
            return true;
        }
        String actual = sha256Hex(file);
        if (!actual.equalsIgnoreCase(expectedSha256.trim())) {
            log.error("Validação pré-restore REPROVADA para {}: esperado={} calculado={}"
                    + " — restauração cancelada sem conexão ao banco.", file, expectedSha256, actual);
            return false;
        }
        log.info("Validação pré-restore aprovada (sha256={}).", actual);
        return true;
    }

    /** Calcula o SHA-256 de um arquivo em streaming, com buffer fixo de 64 KB. */
    public static String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[PUMP_BUFFER_SIZE];
            int read;
            try (InputStream in = Files.newInputStream(file)) {
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /** Remove a senha do alvo de qualquer texto que vá para log ou exceção. */
    protected String sanitize(String text, DatabaseEndpoint endpoint) {
        if (text == null || text.isBlank()) {
            return "(sem saída de erro)";
        }
        String sanitized = text;
        if (endpoint.password() != null && !endpoint.password().isBlank()) {
            sanitized = sanitized.replace(endpoint.password(), REDACTED);
        }
        return sanitized.trim();
    }

    private static String stderrText(ByteArrayOutputStream buffer) {
        String text = buffer.toString(StandardCharsets.UTF_8).trim();
        return text.isEmpty() ? "(sem saída de erro)" : text;
    }

    private static void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
            log.warn("Falha ao fechar o stream de restauração: {}", e.getMessage());
        }
    }

    private static void destroyQuietly(Process process) {
        try {
            process.destroyForcibly();
        } catch (RuntimeException e) {
            log.debug("Falha ao encerrar o processo de restauração: {}", e.getMessage());
        }
    }
}
