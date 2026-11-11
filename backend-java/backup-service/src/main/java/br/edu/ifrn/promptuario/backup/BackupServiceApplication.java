package br.edu.ifrn.promptuario.backup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada do {@code backup-service} — serviço corporativo de backup e
 * recuperação de desastres (Disaster Recovery) do PROMPTUÁRIO.
 *
 * <p>Habilita agendamento ({@link EnableScheduling}) para o ciclo periódico de
 * dumps e processamento assíncrono ({@link EnableAsync}) para que os dumps e
 * uploads concorrentes não bloqueiem a thread de agendamento.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class BackupServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackupServiceApplication.class, args);
    }
}
