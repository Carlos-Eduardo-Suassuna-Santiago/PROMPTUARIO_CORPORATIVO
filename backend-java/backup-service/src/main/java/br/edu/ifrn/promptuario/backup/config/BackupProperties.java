package br.edu.ifrn.promptuario.backup.config;

import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades estruturadas do serviço sob o prefixo {@code promptuario.backup}.
 *
 * <p>Mapeia o agendamento, a política de retenção, a criptografia e a topologia
 * de bancos. A topologia reflete a unificação da Fase 12 — apenas o alvo
 * {@link DatabaseTarget#PATIENT_CLINICAL_DB} para pacientes/prontuários; os hosts
 * órfãos {@code db-patient} e {@code db-clinical} não são mais referenciados.
 *
 * @param scheduleCron      expressão cron do ciclo automático (formato Spring: {@code 0 0 3 * * ?})
 * @param retentionDays     dias de retenção dos artefatos
 * @param rootDir           diretório raiz dos dumps e do manifesto de status
 * @param encryptionEnabled habilita a criptografia autenticada dos artefatos (LGPD)
 * @param encryptionSecret  segredo/chave simétrica usada pela criptografia
 * @param dumpTimeoutSeconds timeout máximo (em segundos) de cada dump antes do
 *                           {@code destroyForcibly()} do processo
 * @param restoreTimeoutSeconds timeout de segurança (em segundos) de cada
 *                           restauração; usado também como orçamento de RTO no drill
 * @param rpoHours          janela de RPO (em horas) usada para atestar a
 *                           atualidade do artefato no drill de contingência
 * @param drillCron         expressão cron do drill mensal de contingência
 * @param postgres          conexões dos bancos PostgreSQL
 * @param mongo             conexão do banco MongoDB
 */
@Validated
@ConfigurationProperties(prefix = "promptuario.backup")
public record BackupProperties(
        @NotBlank String scheduleCron,
        @Min(1) int retentionDays,
        @NotNull Path rootDir,
        boolean encryptionEnabled,
        @NotBlank String encryptionSecret,
        @Min(1) int dumpTimeoutSeconds,
        @Min(1) int restoreTimeoutSeconds,
        @Min(1) int rpoHours,
        @NotBlank String drillCron,
        @NotNull @Valid Postgres postgres,
        @NotNull @Valid Mongo mongo) {

    /**
     * Associa cada {@link DatabaseTarget} PostgreSQL à sua conexão configurada,
     * permitindo que o orquestrador itere sobre os alvos dinamicamente.
     */
    public Map<DatabaseTarget, DatabaseConnection> postgresTargets() {
        Map<DatabaseTarget, DatabaseConnection> targets = new LinkedHashMap<>();
        targets.put(DatabaseTarget.IAM_DB, postgres.iam());
        targets.put(DatabaseTarget.PATIENT_CLINICAL_DB, postgres.patientClinical());
        targets.put(DatabaseTarget.REPORTING_DB, postgres.reporting());
        return targets;
    }

    /** Conexões dos bancos PostgreSQL da topologia unificada. */
    public record Postgres(
            @NotNull @Valid DatabaseConnection iam,
            @NotNull @Valid DatabaseConnection patientClinical,
            @NotNull @Valid DatabaseConnection reporting) {
    }

    /** Conexão do banco MongoDB de documentos da IA. */
    public record Mongo(
            @NotBlank String host,
            @Min(1) int port,
            @NotBlank String dbname,
            @NotBlank String user,
            @NotBlank String password) {
    }

    /** Dados de conexão de um banco PostgreSQL. */
    public record DatabaseConnection(
            @NotBlank String host,
            @Min(1) int port,
            @NotBlank String dbname,
            @NotBlank String user,
            @NotBlank String password) {

        /** Monta a URL JDBC correspondente (útil para verificações de saúde). */
        public String jdbcUrl() {
            return "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
        }
    }
}
