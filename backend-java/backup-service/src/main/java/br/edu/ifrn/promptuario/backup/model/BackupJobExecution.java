package br.edu.ifrn.promptuario.backup.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado consolidado do ciclo completo de backup.
 *
 * <p>Diferente de {@link BackupManifest} (artefato imutável e serializável), esta
 * classe é mutável durante a execução e permite acumular resultados conforme
 * cada alvo termina — inclusive em paralelo —, servindo de base para métricas e
 * para o relatório final de status.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupJobExecution {

    /** Identificador único da execução (correlaciona logs, métricas e manifesto). */
    private String jobId;

    /** Modo que originou a execução. */
    private ExecutionMode mode;

    /** Status consolidado; {@code null} enquanto a execução está em andamento. */
    private BackupStatus status;

    /** Início da execução (UTC). */
    private Instant startedAt;

    /** Término da execução (UTC); {@code null} enquanto em andamento. */
    private Instant finishedAt;

    /** Duração total do ciclo. */
    private Duration duration;

    /** Resultados por alvo, acumulados ao longo da execução. */
    @Builder.Default
    private List<DatabaseBackupResult> results = new ArrayList<>();

    /** Erro fatal do ciclo inteiro (ex.: indisponibilidade do bucket de upload). */
    private String error;

    /** Adiciona (ou substitui) o resultado de um alvo. */
    public synchronized void addResult(DatabaseBackupResult result) {
        if (result == null || result.target() == null) {
            return;
        }
        results.removeIf(existing -> existing.target() == result.target());
        results.add(result);
    }

    /** {@code true} quando a execução já foi encerrada. */
    public boolean isFinished() {
        return finishedAt != null;
    }

    /** Efetiva o término do ciclo, calculando a duração e o status consolidado. */
    public synchronized void finish() {
        this.finishedAt = Instant.now();
        if (startedAt != null) {
            this.duration = Duration.between(startedAt, finishedAt);
        }
        this.status = computeStatus();
    }

    private BackupStatus computeStatus() {
        if (results.isEmpty()) {
            return BackupStatus.FAILED;
        }
        long failures = results.stream().filter(r -> !r.isSuccess()).count();
        if (failures == 0) {
            return BackupStatus.SUCCESS;
        }
        return failures == results.size() ? BackupStatus.FAILED : BackupStatus.PARTIAL;
    }
}
