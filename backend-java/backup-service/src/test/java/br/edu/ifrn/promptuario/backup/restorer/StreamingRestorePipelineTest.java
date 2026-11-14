package br.edu.ifrn.promptuario.backup.restorer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.model.DatabaseEndpoint;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.service.CryptoService;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import javax.crypto.CipherOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes do pipeline de restauração segura.
 *
 * <p>Nenhum PostgreSQL é necessário: um restaurador sintético ({@code sh -c})
 * consome o {@code stdin} e grava o conteúdo restaurado em disco, permitindo
 * provar o pipeline completo (decifra + descomprime + stdin) e, principalmente,
 * que arquivos <strong>corrompidos/adulterados</strong> são barrados.
 */
class StreamingRestorePipelineTest {

    private static final String SECRET = "PROMPTUARIO_AES_256_SECRET_KEY_SAMPLE_32B";
    private static final String SQL = "CREATE TABLE paciente (id int);\nINSERT INTO paciente VALUES (1);\n";
    private static final String PASSWORD = "sup3r-s3cret";

    @TempDir
    Path rootDir;

    private CryptoService cryptoService;
    private DatabaseEndpoint endpoint;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService(properties(30));
        endpoint = new DatabaseEndpoint("localhost", 5432, "clinical", PASSWORD, "patient_clinical_db");
    }

    private BackupProperties properties(int restoreTimeoutSeconds) {
        BackupProperties.DatabaseConnection connection =
                new BackupProperties.DatabaseConnection("localhost", 5432, "patient_clinical_db", "clinical", PASSWORD);
        return new BackupProperties(
                "0 0 3 * * ?", 7, rootDir, true, SECRET, 30, restoreTimeoutSeconds, 24, "0 0 4 1 * ?",
                new BackupProperties.Postgres(connection, connection, connection),
                new BackupProperties.Mongo("localhost", 27017, "ai_db", "ai", "ai_pass"));
    }

    /** Gera um artefato real: gzip → AES-256-GCM → arquivo (mesmo formato do dumper). */
    private Path writeArtifact(Path target, String content) throws Exception {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        try (OutputStream fileOut = Files.newOutputStream(target);
             CipherOutputStream cipherOut = cryptoService.createEncryptingStream(fileOut, cryptoService.activeKey());
             GZIPOutputStream gzipOut = new GZIPOutputStream(cipherOut)) {
            gzipOut.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return target;
    }

    private Path artifact() throws Exception {
        return writeArtifact(rootDir.resolve("artifact/postgres.sql.gz.enc"), SQL);
    }

    private ScriptedRestorer restorer(String outputFile) {
        return new ScriptedRestorer(properties(30), cryptoService, endpoint, "cat > " + outputFile);
    }

    private static void corruptLastByte(Path artifact) throws Exception {
        byte[] bytes = Files.readAllBytes(artifact);
        bytes[bytes.length - 1] ^= 0x7F; // corrompe a tag de autenticação GCM
        Files.write(artifact, bytes);
    }

    @Test
    @DisplayName("restaura artefato íntegro: decifra, descomprime e entrega o SQL ao stdin")
    void restoresIntactArtifact() throws Exception {
        Path artifact = artifact();
        Path output = rootDir.resolve("restored.sql");
        String sha256 = AbstractStreamingDatabaseRestorer.sha256Hex(artifact);

        boolean restored = restorer(output.toString()).restore(DatabaseTarget.PATIENT_CLINICAL_DB, artifact, sha256);

        assertThat(restored).isTrue();
        assertThat(Files.readString(output, StandardCharsets.UTF_8)).isEqualTo(SQL);
    }

    @Test
    @DisplayName("princípio 1 — SHA-256 divergente cancela a restauração sem abrir conexão")
    void blocksOnChecksumMismatch() throws Exception {
        Path artifact = artifact();
        String originalSha = AbstractStreamingDatabaseRestorer.sha256Hex(artifact);
        corruptLastByte(artifact);
        Path output = rootDir.resolve("restored.sql");

        boolean restored =
                restorer(output.toString()).restore(DatabaseTarget.PATIENT_CLINICAL_DB, artifact, originalSha);

        assertThat(restored).isFalse();
        assertThat(Files.exists(output)).isFalse(); // nenhum processo/conexão foi iniciado
    }

    @Test
    @DisplayName("princípio 2 — tag GCM inválida barra a restauração mesmo com SHA-256 conferido")
    void blocksOnInvalidGcmTag() throws Exception {
        Path artifact = artifact();
        corruptLastByte(artifact);
        // Passa o hash do arquivo JÁ corrompido: a validação prévia aprova e quem
        // barra a restauração é a autenticação AES-GCM.
        String corruptedSha = AbstractStreamingDatabaseRestorer.sha256Hex(artifact);
        Path output = rootDir.resolve("restored.sql");

        assertThatThrownBy(() -> restorer(output.toString())
                .restore(DatabaseTarget.PATIENT_CLINICAL_DB, artifact, corruptedSha))
                .isInstanceOf(RestoreFailedException.class)
                .hasMessageContaining("incompleto");
    }

    @Test
    @DisplayName("artefato inexistente retorna false sem executar a restauração")
    void returnsFalseWhenArtifactMissing() {
        Path missing = rootDir.resolve("nao-existe.sql.gz.enc");

        boolean restored = restorer(rootDir.resolve("out.sql").toString())
                .restore(DatabaseTarget.PATIENT_CLINICAL_DB, missing, null);

        assertThat(restored).isFalse();
    }

    @Test
    @DisplayName("falha da ferramenta lança RestoreFailedException com o stderr")
    void failsWhenCommandReturnsNonZero() throws Exception {
        Path artifact = artifact();
        String sha256 = AbstractStreamingDatabaseRestorer.sha256Hex(artifact);
        ScriptedRestorer failing = new ScriptedRestorer(properties(30), cryptoService, endpoint,
                "cat > /dev/null; echo 'erro de restauracao' >&2; exit 7");

        assertThatThrownBy(() -> failing.restore(DatabaseTarget.PATIENT_CLINICAL_DB, artifact, sha256))
                .isInstanceOf(RestoreFailedException.class)
                .hasMessageContaining("código 7")
                .hasMessageContaining("erro de restauracao");
    }

    @Test
    @DisplayName("mensagem de erro nunca vaza a senha do alvo")
    void neverLeaksCredentials() throws Exception {
        Path artifact = artifact();
        String sha256 = AbstractStreamingDatabaseRestorer.sha256Hex(artifact);
        ScriptedRestorer failing = new ScriptedRestorer(properties(30), cryptoService, endpoint,
                "echo 'autenticacao falhou para " + PASSWORD + "' >&2; exit 9");

        assertThatThrownBy(() -> failing.restore(DatabaseTarget.PATIENT_CLINICAL_DB, artifact, sha256))
                .isInstanceOf(RestoreFailedException.class)
                .hasMessageNotContaining(PASSWORD)
                .hasMessageContaining("***");
    }

    @Test
    @DisplayName("timeout de segurança encerra uma restauração travada")
    void enforcesSafetyTimeout() throws Exception {
        Path artifact = artifact();
        String sha256 = AbstractStreamingDatabaseRestorer.sha256Hex(artifact);
        ScriptedRestorer hanging =
                new ScriptedRestorer(properties(1), cryptoService, endpoint, "exec sleep 30");

        long startMillis = System.currentTimeMillis();
        assertThatThrownBy(() -> hanging.restore(DatabaseTarget.PATIENT_CLINICAL_DB, artifact, sha256))
                .isInstanceOf(RestoreFailedException.class)
                .hasMessageContaining("Timeout");
        assertThat(System.currentTimeMillis() - startMillis).isLessThan(20_000L);
    }

    /** Restaurador sintético: executa um comando shell arbitrário consumindo o stdin. */
    private static final class ScriptedRestorer extends AbstractStreamingDatabaseRestorer {

        private final DatabaseEndpoint endpoint;
        private final String shellCommand;

        ScriptedRestorer(
                BackupProperties backupProperties,
                CryptoService cryptoService,
                DatabaseEndpoint endpoint,
                String shellCommand) {
            super(backupProperties, cryptoService);
            this.endpoint = endpoint;
            this.shellCommand = shellCommand;
        }

        @Override
        protected DatabaseEndpoint endpoint(DatabaseTarget target) {
            return endpoint;
        }

        @Override
        protected List<String> buildCommand(DatabaseEndpoint ignored) {
            return List.of("sh", "-c", shellCommand);
        }

        @Override
        protected Map<String, String> environment(DatabaseEndpoint ignored) {
            return Map.of();
        }

        @Override
        protected boolean compressionEnabled() {
            return true;
        }

        @Override
        protected String toolName() {
            return "scripted-restore";
        }
    }
}
