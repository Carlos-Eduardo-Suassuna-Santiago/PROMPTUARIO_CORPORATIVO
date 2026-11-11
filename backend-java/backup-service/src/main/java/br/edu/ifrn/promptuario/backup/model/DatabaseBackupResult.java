package br.edu.ifrn.promptuario.backup.model;

/**
 * Resultado individual do dump de um alvo.
 *
 * <p>É imutável por natureza ({@code record}) e expõe fábricas que garantem a
 * consistência entre {@link BackupStatus} e os campos preenchidos — um resultado
 * bem-sucedido sempre possui caminho de arquivo e hash SHA-256; um resultado
 * falho sempre possui mensagem de erro.
 *
 * @param target          alvo de origem do dump ({@code null} apenas em cenários de falha precoce)
 * @param kind            natureza técnica do artefato (PostgreSQL ou MongoDB)
 * @param status          {@link BackupStatus#SUCCESS} ou {@link BackupStatus#FAILED}
 * @param filePath        caminho absoluto do artefato no disco ({@code null} em falha)
 * @param s3Key           chave do objeto no bucket MinIO ({@code null} se o upload não ocorreu)
 * @param sizeBytes       tamanho do artefato em bytes
 * @param durationSeconds duração do dump/upload em segundos
 * @param sha256Hex       hash SHA-256 (hex) para verificação de integridade ({@code null} em falha)
 * @param error           mensagem de erro ({@code null} em sucesso)
 */
public record DatabaseBackupResult(
        DatabaseTarget target,
        BackupKind kind,
        BackupStatus status,
        String filePath,
        String s3Key,
        long sizeBytes,
        double durationSeconds,
        String sha256Hex,
        String error) {

    /** Cria um resultado de sucesso (dump gerado e, opcionalmente, enviado ao MinIO). */
    public static DatabaseBackupResult success(
            DatabaseTarget target,
            BackupKind kind,
            String filePath,
            String s3Key,
            long sizeBytes,
            double durationSeconds,
            String sha256Hex) {
        return new DatabaseBackupResult(
                target, kind, BackupStatus.SUCCESS, filePath, s3Key, sizeBytes, durationSeconds, sha256Hex, null);
    }

    /** Cria um resultado de falha isolada, preservando a causa para o manifesto. */
    public static DatabaseBackupResult failure(DatabaseTarget target, BackupKind kind, String error) {
        return new DatabaseBackupResult(target, kind, BackupStatus.FAILED, null, null, 0L, 0.0d, null, error);
    }

    /** {@code true} quando o dump foi concluído com sucesso. */
    public boolean isSuccess() {
        return status == BackupStatus.SUCCESS;
    }

    /** Tamanho do artefato em megabytes, arredondado a duas casas decimais. */
    public double sizeMegabytes() {
        return Math.round((sizeBytes / 1_048_576.0d) * 100.0d) / 100.0d;
    }
}
