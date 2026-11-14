package br.edu.ifrn.promptuario.backup.model;

/**
 * Alvos de backup gerenciados pelo serviço.
 *
 * <p>A topologia reflete a unificação da Fase 12: os antigos serviços
 * {@code patient-service} e {@code clinical-service} foram fundidos, portanto
 * existe apenas o alvo {@link #PATIENT_CLINICAL_DB} (container
 * {@code db-patient-clinical}, banco {@code patient_clinical_db}). Os hosts
 * órfãos {@code db-patient} e {@code db-clinical} não existem mais.
 */
public enum DatabaseTarget {

    /** Banco relacional de identidade e acesso — {@code db-iam}/{@code iam_db}. */
    IAM_DB,

    /** Banco relacional unificado de pacientes e prontuários — {@code db-patient-clinical}/{@code patient_clinical_db}. */
    PATIENT_CLINICAL_DB,

    /** Banco relacional de relatórios e auditoria — {@code db-reporting}/{@code reporting_db}. */
    REPORTING_DB,

    /** Banco de documentos da IA — {@code db-ai}/{@code ai_db} (MongoDB). */
    MONGO_AI_DB,

    /** Buckets de artefatos no MinIO/S3 (não é um banco de dados). */
    MINIO_BUCKETS
}
