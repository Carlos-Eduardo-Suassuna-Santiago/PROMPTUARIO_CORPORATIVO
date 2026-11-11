package br.edu.ifrn.promptuario.backup.dumper;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.model.BackupKind;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.service.CryptoService;
import br.edu.ifrn.promptuario.backup.storage.S3BackupStorageService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Dumper dos bancos PostgreSQL (<b>IAM</b>, <b>Patient-Clinical</b> e
 * <b>Reporting</b>) via {@code pg_dump}.
 *
 * <p>A senha é injetada exclusivamente pela variável de ambiente
 * {@code PGPASSWORD}, nunca na linha de comando — assim ela não aparece em
 * {@code ps}, no histórico do shell nem em logs de auditoria de processos.
 */
@Component
public class PostgresDatabaseDumper extends AbstractStreamingDatabaseDumper {

    public PostgresDatabaseDumper(
            BackupProperties backupProperties,
            CryptoService cryptoService,
            S3BackupStorageService storageService) {
        super(backupProperties, cryptoService, storageService);
    }

    @Override
    protected BackupKind kind(DatabaseTarget target) {
        return BackupKind.POSTGRES;
    }

    @Override
    protected DatabaseEndpoint endpoint(DatabaseTarget target) {
        BackupProperties.DatabaseConnection connection = backupProperties.postgresTargets().get(target);
        if (connection == null) {
            throw new IllegalArgumentException("Alvo não suportado pelo dumper PostgreSQL: " + target);
        }
        return new DatabaseEndpoint(
                connection.host(), connection.port(), connection.user(),
                connection.password(), connection.dbname());
    }

    @Override
    protected List<String> buildCommand(DatabaseTarget target, DatabaseEndpoint endpoint) {
        return List.of(
                "pg_dump",
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
