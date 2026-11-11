package br.edu.ifrn.promptuario.backup.model;

import java.time.Instant;
import java.util.List;

/**
 * Manifesto consolidado de um ciclo de backup, persistido em disco
 * ({@code /var/backups/status.json}) e enviado ao MinIO.
 *
 * @param backupId            identificador único da execução
 * @param generatedAt         instante de geração do manifesto (UTC)
 * @param schemaVersion       versão do schema do manifesto, para evolução futura
 * @param databases           resultados individuais por alvo
 * @param encryptionAlgorithm algoritmo de criptografia aplicado (ex.: {@code AES-256-GCM} ou {@code NONE})
 * @param rpoMet              {@code true} se o objetivo de ponto de recuperação (RPO) foi cumprido
 * @param retentionUntil      instante até o qual os artefatos devem ser retidos
 */
public record BackupManifest(
        String backupId,
        Instant generatedAt,
        String schemaVersion,
        List<DatabaseBackupResult> databases,
        String encryptionAlgorithm,
        boolean rpoMet,
        Instant retentionUntil) {

    /** Versão corrente do schema do manifesto. */
    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    /** Garante imutabilidade e ausência de {@code null} na coleção de resultados. */
    public BackupManifest {
        databases = databases == null ? List.of() : List.copyOf(databases);
    }

    /** Quantidade de alvos presentes no manifesto. */
    public int databaseCount() {
        return databases.size();
    }

    /** {@code true} quando todos os alvos do ciclo foram concluídos com sucesso. */
    public boolean allSucceeded() {
        return !databases.isEmpty() && databases.stream().allMatch(DatabaseBackupResult::isSuccess);
    }
}
