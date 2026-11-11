package br.edu.ifrn.promptuario.backup.dumper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.model.BackupKind;
import br.edu.ifrn.promptuario.backup.model.BackupStatus;
import br.edu.ifrn.promptuario.backup.model.DatabaseBackupResult;
import br.edu.ifrn.promptuario.backup.model.DatabaseEndpoint;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.model.ExecutionMode;
import br.edu.ifrn.promptuario.backup.service.CryptoService;
import br.edu.ifrn.promptuario.backup.storage.S3BackupStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.crypto.CipherInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Prova o pipeline completo <em>sem</em> PostgreSQL, MongoDB ou MinIO: um
 * comando real ({@code sh}) é executado, seu stdout atravessa
 * gzip → AES-256-GCM → SHA-256 → disco, e o resultado é decifrado e
 * descomprimido de volta, validando IV, tag GCM, hash e limpeza em falha.
 */
class StreamingDumpPipelineTest {

    private static final String SECRET = "PROMPTUARIO_AES_256_SECRET_KEY_SAMPLE_32B";
    private static final String PAYLOAD = "PROMPTUARIO;-- dump sintetico de validacao do pipeline";

    @TempDir
    Path rootDir;

    private BackupProperties properties;
    private CryptoService cryptoService;
    private S3BackupStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = properties(true);
        cryptoService = new CryptoService(properties);
        storageService = mock(S3BackupStorageService.class);
        when(storageService.buildObjectKey(any(BackupKind.class), anyString(), anyString()))
                .thenAnswer(invocation -> "backups/test/" + invocation.getArgument(2));
        when(storageService.uploadArtifact(any(Path.class), anyString(), anyMap()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private BackupProperties properties(boolean encryptionEnabled) {
        BackupProperties.DatabaseConnection connection =
                new BackupProperties.DatabaseConnection("localhost", 5432, "iam_db", "iam", "iam_pass");
        return new BackupProperties(
                "0 0 3 * * ?", 7, rootDir, encryptionEnabled, SECRET, 30, 30, 24, "0 0 4 1 * ?",
                new BackupProperties.Postgres(connection, connection, connection),
                new BackupProperties.Mongo("localhost", 27017, "ai_db", "ai", "ai_pass"));
    }

    @Test
    @DisplayName("executa o pipeline, grava o artefato cifrado e recupera o conteúdo original")
    void streamsDumpEncryptsAndDigests() throws Exception {
        ScriptedDumper dumper = new ScriptedDumper(properties, cryptoService, storageService, PAYLOAD);

        DatabaseBackupResult result = dumper.dump(DatabaseTarget.IAM_DB, ExecutionMode.CLI);

        assertThat(result.status()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(result.sha256Hex()).hasSize(64);
        assertThat(result.filePath()).endsWith(".sql.gz.enc");

        Path artifact = Path.of(result.filePath());
        byte[] stored = Files.readAllBytes(artifact);
        assertThat(result.sizeBytes()).isEqualTo(stored.length);

        // O SHA-256 registrado é o do arquivo exatamente como está em disco.
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(stored));
        assertThat(result.sha256Hex()).isEqualTo(expectedHash);

        // O arquivo não contém o payload em texto claro.
        assertThat(new String(stored, StandardCharsets.UTF_8)).doesNotContain("PROMPTUARIO;--");

        // Decifra (lendo o IV do prefixo) e descomprime: conteúdo íntegro.
        byte[] decrypted;
        try (InputStream fileIn = Files.newInputStream(artifact);
             CipherInputStream cipherIn = cryptoService.createDecryptingStream(fileIn, cryptoService.activeKey())) {
            decrypted = cipherIn.readAllBytes();
        }
        assertThat(new String(gunzip(decrypted), StandardCharsets.UTF_8)).isEqualTo(PAYLOAD);
    }

    @Test
    @DisplayName("gera um IV novo (nunca reutilizado) para cada artefato")
    void neverReusesIv() throws Exception {
        ScriptedDumper dumper = new ScriptedDumper(properties, cryptoService, storageService, PAYLOAD);

        byte[] first = Files.readAllBytes(Path.of(dumper.dump(DatabaseTarget.IAM_DB, ExecutionMode.CLI).filePath()));
        byte[] second =
                Files.readAllBytes(Path.of(dumper.dump(DatabaseTarget.REPORTING_DB, ExecutionMode.CLI).filePath()));

        assertThat(Arrays.copyOf(second, CryptoService.GCM_IV_LENGTH))
                .isNotEqualTo(Arrays.copyOf(first, CryptoService.GCM_IV_LENGTH));
    }

    @Test
    @DisplayName("detecta adulteração do artefato (tag de autenticação GCM)")
    void detectsTamperedArtifact() throws Exception {
        ScriptedDumper dumper = new ScriptedDumper(properties, cryptoService, storageService, PAYLOAD);
        Path artifact = Path.of(dumper.dump(DatabaseTarget.IAM_DB, ExecutionMode.CLI).filePath());

        byte[] tampered = Files.readAllBytes(artifact);
        tampered[tampered.length - 1] ^= 0x7F; // corrompe a tag de autenticação

        try (CipherInputStream cipherIn = cryptoService.createDecryptingStream(
                new ByteArrayInputStream(tampered), cryptoService.activeKey())) {
            assertThatThrownBy(cipherIn::readAllBytes).isInstanceOf(IOException.class);
        }
    }

    @Test
    @DisplayName("falha do comando gera FAILED, preserva a mensagem e remove o artefato parcial")
    void failingCommandIsIsolated() throws Exception {
        ScriptedDumper dumper =
                new ScriptedDumper(properties, cryptoService, storageService, null); // comando que falha

        DatabaseBackupResult result = dumper.dump(DatabaseTarget.IAM_DB, ExecutionMode.CLI);

        assertThat(result.status()).isEqualTo(BackupStatus.FAILED);
        assertThat(result.error()).contains("código 3").contains("boom");

        // Nenhum artefato (mesmo parcial/corrompido) pode permanecer em disco.
        List<Path> remainingArtifacts;
        try (Stream<Path> paths = Files.walk(rootDir)) {
            remainingArtifacts = paths.filter(Files::isRegularFile).toList();
        }
        assertThat(remainingArtifacts).isEmpty();
    }

    private static byte[] gunzip(byte[] compressed) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return gzip.readAllBytes();
        }
    }

    /** Dumper sintético: emite o payload quando ele existe, ou falha com código 3. */
    private static final class ScriptedDumper extends AbstractStreamingDatabaseDumper {

        private final String payload;

        ScriptedDumper(
                BackupProperties backupProperties,
                CryptoService cryptoService,
                S3BackupStorageService storageService,
                String payload) {
            super(backupProperties, cryptoService, storageService);
            this.payload = payload;
        }

        @Override
        protected BackupKind kind(DatabaseTarget target) {
            return BackupKind.POSTGRES;
        }

        @Override
        protected DatabaseEndpoint endpoint(DatabaseTarget target) {
            return new DatabaseEndpoint("localhost", 5432, "iam", "iam_pass", target.name().toLowerCase());
        }

        @Override
        protected List<String> buildCommand(DatabaseTarget target, DatabaseEndpoint endpoint) {
            if (payload == null) {
                return List.of("sh", "-c", "echo boom >&2; exit 3");
            }
            return List.of("sh", "-c", "printf %s '" + payload + "'");
        }

        @Override
        protected Map<String, String> environment(DatabaseEndpoint endpoint) {
            return Map.of();
        }

        @Override
        protected String artifactPrefix() {
            return "postgres";
        }

        @Override
        protected String artifactExtension() {
            return ".sql.gz";
        }

        @Override
        protected boolean compressionEnabled() {
            return true;
        }
    }
}
