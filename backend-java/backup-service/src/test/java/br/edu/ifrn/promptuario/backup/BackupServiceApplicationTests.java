package br.edu.ifrn.promptuario.backup;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import br.edu.ifrn.promptuario.backup.config.StorageProperties;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Valida que o contexto sobe e que a topologia unificada (Fase 12) é vinculada
 * corretamente a partir do {@code application.yml}.
 */
@SpringBootTest
class BackupServiceApplicationTests {

    @Autowired
    private BackupProperties backupProperties;

    @Autowired
    private StorageProperties storageProperties;

    @Test
    void contextLoads() {
        assertThat(backupProperties).isNotNull();
        assertThat(storageProperties).isNotNull();
    }

    @Test
    void bindsUnifiedTopology() {
        var targets = backupProperties.postgresTargets();

        assertThat(targets).containsOnlyKeys(
                DatabaseTarget.IAM_DB,
                DatabaseTarget.PATIENT_CLINICAL_DB,
                DatabaseTarget.REPORTING_DB);

        assertThat(targets.get(DatabaseTarget.IAM_DB).host()).isEqualTo("db-iam");
        assertThat(targets.get(DatabaseTarget.PATIENT_CLINICAL_DB).host()).isEqualTo("db-patient-clinical");
        assertThat(targets.get(DatabaseTarget.PATIENT_CLINICAL_DB).dbname()).isEqualTo("patient_clinical_db");
        assertThat(targets.get(DatabaseTarget.PATIENT_CLINICAL_DB).user()).isEqualTo("clinical");
        assertThat(targets.get(DatabaseTarget.REPORTING_DB).host()).isEqualTo("db-reporting");

        // Os hosts órfãos legados não podem reaparecer na topologia.
        assertThat(targets.values())
                .extracting(BackupProperties.DatabaseConnection::host)
                .doesNotContain("db-patient", "db-clinical");
    }

    @Test
    void bindsSchedulingAndRetention() {
        assertThat(backupProperties.scheduleCron()).isEqualTo("0 0 3 * * ?");
        assertThat(backupProperties.retentionDays()).isEqualTo(7);
        assertThat(backupProperties.rootDir().toString()).isEqualTo("/var/backups");
    }

    @Test
    void bindsMongoAndStorage() {
        assertThat(backupProperties.mongo().host()).isEqualTo("db-ai");
        assertThat(backupProperties.mongo().dbname()).isEqualTo("ai_db");
        assertThat(storageProperties.bucketBackups()).isEqualTo("backups");
        assertThat(storageProperties.endpoint()).isEqualTo("http://minio:9000");
        assertThat(storageProperties.region()).isEqualTo("us-east-1");
    }
}
