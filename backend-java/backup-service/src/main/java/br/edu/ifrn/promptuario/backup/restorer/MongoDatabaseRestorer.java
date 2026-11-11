package br.edu.ifrn.promptuario.backup.restorer;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.model.DatabaseEndpoint;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.service.CryptoService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Restaurador do MongoDB de documentos da IA via {@code mongorestore}.
 *
 * <p>O arquivo é decifrado e enviado pelo {@code stdin}; o
 * {@code --archive --gzip --drop} faz o {@code mongorestore} ler o archive
 * comprimido e substituir o conteúdo do banco de destino. Como o gzip já está
 * embutido no archive, o pipeline <strong>não</strong> descomprime novamente.
 */
@Component
public class MongoDatabaseRestorer extends AbstractStreamingDatabaseRestorer {

    public MongoDatabaseRestorer(BackupProperties backupProperties, CryptoService cryptoService) {
        super(backupProperties, cryptoService);
    }

    @Override
    protected DatabaseEndpoint endpoint(DatabaseTarget target) {
        if (target != DatabaseTarget.MONGO_AI_DB) {
            throw new IllegalArgumentException("Alvo não suportado pelo restaurador MongoDB: " + target);
        }
        BackupProperties.Mongo mongo = backupProperties.mongo();
        return new DatabaseEndpoint(mongo.host(), mongo.port(), mongo.user(), mongo.password(), mongo.dbname());
    }

    @Override
    protected List<String> buildCommand(DatabaseEndpoint endpoint) {
        return List.of(
                "mongorestore",
                "--host=" + endpoint.host(),
                "--port=" + endpoint.port(),
                "--username=" + endpoint.user(),
                "--password=" + endpoint.password(),
                "--authenticationDatabase=admin",
                "--db=" + endpoint.dbname(),
                "--archive",
                "--gzip",
                "--drop");
    }

    @Override
    protected Map<String, String> environment(DatabaseEndpoint endpoint) {
        return Map.of();
    }

    @Override
    protected boolean compressionEnabled() {
        return false; // o archive do mongodump já é gzip
    }

    @Override
    protected String toolName() {
        return "mongorestore";
    }
}
