package br.edu.ifrn.promptuario.backup.cli;

import br.edu.ifrn.promptuario.backup.model.BackupKind;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.service.BackupOrchestratorService;
import br.edu.ifrn.promptuario.backup.service.RestoreOrchestratorService;
import br.edu.ifrn.promptuario.backup.model.ExecutionMode;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Point de entrada de linha de comando (CLI) do {@code backup-service}.
 *
 * <p>Suporta três modos de operação:
 * <ul>
 *   <li>{@code --once}: executa um backup completo manual e encerra com {@code System.exit(0)}
 *       em caso de sucesso ou {@code System.exit(1)} em caso de falha.</li>
 *   <li>{@code --restore --kind <postgres|mongo> --target <alvo> --file <caminho>}:
 *       executa restauração controlada e encerra com status correspondente.</li>
 *   <li>Sem argumentos: inicia como daemon, servindo a API REST na porta 8000 e
 *       rodando o agendador periódico {@code @Scheduled}.</li>
 * </ul>
 */
@Component
public class BackupCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BackupCommandLineRunner.class);

    @Autowired(required = false)
    private BackupOrchestratorService backupOrchestratorService;

    @Autowired(required = false)
    private RestoreOrchestratorService restoreOrchestratorService;

    @Command(name = "backup-service", mixinStandardHelpOptions = true, version = "1.0.0-java",
            description = "Serviço de backup e DR do Promptuário Corporativo")
    private static class BackupCommands {

        @Option(names = {"--once"}, description = "Dispara backup completo manual imediatamente")
        boolean once;

        @Option(names = {"--restore"}, description = "Executa restauração controlada de um artefato")
        boolean restore;

        @Option(names = {"--kind"}, description = "Tipo do banco: postgres ou mongo",
                paramLabel = "<postgres|mongo>")
        String kind;

        @Option(names = {"--target"}, description = "Nome ou enum do alvo de restauração",
                paramLabel = "<target>")
        String target;

        @Option(names = {"--file"}, description = "Caminho do artefato de backup",
                paramLabel = "<path>")
        String file;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args == null || args.length == 0) {
            // Modo daemon: apenas loga; a aplicação Spring já sobe a API REST e o scheduler.
            log.info("Modo daemon iniciado: API REST na porta 8000 e agendador @Scheduled ativos.");
            return;
        }

        CommandLine cmd = new CommandLine(new BackupCommands());
        cmd.parseArgs(args);

        BackupCommands commands = (BackupCommands) cmd.getCommand().getUserObject();

        if (commands.once) {
            runOnce();
            return;
        }

        if (commands.restore) {
            runRestore(commands.kind, commands.target, commands.file);
            return;
        }

        log.warn("Argumentos não reconhecidos: {}. Use --help para listar as opções.", (Object) args);
    }

    private void runOnce() {
        if (backupOrchestratorService == null) {
            log.error("BackupOrchestratorService não disponível para execução --once.");
            System.exit(1);
        }
        try {
            log.info("Iniciando backup completo manual (--once)...");
            backupOrchestratorService.executeFullBackup(ExecutionMode.FULL);
            log.info("Backup completo finalizado com sucesso.");
            System.exit(0);
        } catch (Exception e) {
            log.error("Falha no backup manual (--once): {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private void runRestore(String kind, String target, String file) {
        if (restoreOrchestratorService == null) {
            log.error("RestoreOrchestratorService não disponível para execução --restore.");
            System.exit(1);
        }
        if (kind == null || target == null || file == null) {
            log.error("Para --restore são obrigatórios: --kind, --target e --file.");
            System.exit(1);
        }

        BackupKind backupKind;
        try {
            backupKind = BackupKind.valueOf(kind.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Tipo de banco inválido '{}'. Use 'postgres' ou 'mongo'.", kind);
            System.exit(1);
            return;
        }

        try {
            log.info("Iniciando restauração controlada: kind={}, target={}, file={}",
                    backupKind, target, file);
            RestoreOrchestratorService.RestoreOutcome outcome =
                    restoreOrchestratorService.restoreDatabase(target, file, null);
            if (outcome.success()) {
                log.info("Restauração concluída com sucesso em {} ms", outcome.durationMillis());
                System.exit(0);
            } else {
                log.error("Restauração falhou: {}", outcome.message());
                System.exit(1);
            }
        } catch (Exception e) {
            log.error("Erro inesperado na restauração: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
