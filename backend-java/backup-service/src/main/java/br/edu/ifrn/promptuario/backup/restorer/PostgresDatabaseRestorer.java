package br.edu.ifrn.promptuario.backup.restorer;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.model.DatabaseEndpoint;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.service.CryptoService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Restaurador dos bancos PostgreSQL via {@code psql}.
 *
 * <p>A senha é injetada exclusivamente por {@code PGPASSWORD}, mantendo-a fora
 * da linha de comando (e, portanto, de {@code ps} e dos logs).
 *
 * <p><b>Recomendação operacional:</b> para restaurações de produção, avaliar
 * {@code psql --single-transaction} — sem ele, uma falha no meio do stream pode
 * deixar o banco parcialmente aplicado. O envelope de segurança padrão é a
 * validação SHA-256 pré-restore, que barra o artefato antes de qualquer conexão.
 */
@Component
public class PostgresDatabaseRestorer extends AbstractStreamingDatabaseRestorer {

    public PostgresDatabaseRestorer(BackupProperties backupProperties, CryptoService cryptoService) {
        super(backupProperties, cryptoService);
    }

    @Override
    protected DatabaseEndpoint endpoint(DatabaseTarget target) {
        BackupProperties.DatabaseConnection connection = backupProperties.postgresTargets().get(target);
        if (connection == null) {
            throw new IllegalArgumentException("Alvo não suportado pelo restaurador PostgreSQL: " + target);
        }
        return new DatabaseEndpoint(
                connection.host(), connection.port(), connection.user(),
                connection.password(), connection.dbname());
    }

    @Override
    protected List<String> buildCommand(DatabaseEndpoint endpoint) {
        return List.of(
                "psql",
                "-h", endpoint.host(),
                "-p", String.valueOf(endpoint.port()),
                "-U", endpoint.user(),
                endpoint.dbname());
    }

    @Override
    protected Map<String, String> environment(DatabaseEndpoint endpoint) {
        return Map.of("PGPASSWORD", endpoint.password());
    }

    @Override
    protected boolean compressionEnabled() {
        return true; // o dump foi comprimido com gzip antes de cifrar
    }

    @Override
    protected String toolName() {
        return "psql";
    }
}
