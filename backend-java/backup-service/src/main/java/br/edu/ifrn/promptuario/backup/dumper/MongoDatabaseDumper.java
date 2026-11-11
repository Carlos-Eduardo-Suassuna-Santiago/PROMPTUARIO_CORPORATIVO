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
 * Dumper do MongoDB de documentos da IA via {@code mongodump --archive --gzip}.
 *
 * <p><b>Nota de segurança:</b> o {@code mongodump} não suporta a injeção de
 * senha por variável de ambiente (diferente do {@code PGPASSWORD} do
 * {@code pg_dump}), portanto o segredo trafega em {@code --password}. Como
 * mitigação, a linha de comando nunca é registrada em log — apenas o alvo e o
 * caminho do artefato.
 *
 * <p>Como o {@code --gzip} já comprime o arquivo, o {@link #compressionEnabled()}
 * é {@code false}: uma segunda passagem de gzip corromperia o formato que o
 * {@code mongorestore} espera.
 */
@Component
public class MongoDatabaseDumper extends AbstractStreamingDatabaseDumper {

    public MongoDatabaseDumper(
            BackupProperties backupProperties,
            CryptoService cryptoService,
            S3BackupStorageService storageService) {
        super(backupProperties, cryptoService, storageService);
    }

    @Override
    protected BackupKind kind(DatabaseTarget target) {
        return BackupKind.MONGO;
    }

    @Override
    protected DatabaseEndpoint endpoint(DatabaseTarget target) {
        if (target != DatabaseTarget.MONGO_AI_DB) {
            throw new IllegalArgumentException("Alvo não suportado pelo dumper MongoDB: " + target);
        }
        BackupProperties.Mongo mongo = backupProperties.mongo();
        return new DatabaseEndpoint(mongo.host(), mongo.port(), mongo.user(), mongo.password(), mongo.dbname());
    }

    @Override
    protected List<String> buildCommand(DatabaseTarget target, DatabaseEndpoint endpoint) {
        return List.of(
                "mongodump",
                "--host=" + endpoint.host(),
                "--port=" + endpoint.port(),
                "--username=" + endpoint.user(),
                "--password=" + endpoint.password(),
                "--authenticationDatabase=admin",
                "--db=" + endpoint.dbname(),
                "--archive",
                "--gzip");
    }

    @Override
    protected Map<String, String> environment(DatabaseEndpoint endpoint) {
        return Map.of();
    }

    @Override
    protected String artifactPrefix() {
        return "mongo";
    }

    @Override
    protected String artifactExtension() {
        return ".archive.gz";
    }

    @Override
    protected boolean compressionEnabled() {
        return false;
    }
}
