package br.edu.ifrn.promptuario.backup.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Infraestrutura de execução assíncrona do serviço.
 *
 * <p>Define um {@link ThreadPoolTaskScheduler} dedicado ao agendamento do ciclo
 * de backup e um {@link ThreadPoolTaskExecutor} próprio para os dumps paralelos,
 * evitando que a execução dos dumps concorra com a thread de agendamento (e
 * impedindo o starvation do pool padrão do Spring Boot).
 */
@Configuration
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Scheduler dedicado ao ciclo agendado ({@code promptuario.backup.schedule-cron}).
     * Pool pequeno de propósito: o agendador apenas dispara o ciclo, quem executa
     * os dumps é o {@link #backupTaskExecutor()}.
     */
    @Bean("backupTaskScheduler")
    public ThreadPoolTaskScheduler backupTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("backup-sched-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setErrorHandler(throwable -> log.error("Falha em tarefa agendada de backup", throwable));
        return scheduler;
    }

    /**
     * Executor dedicado aos dumps e uploads concorrentes. É o executor padrão
     * utilizado por {@code @Async}, mantendo os jobs de backup isolados das
     * demais tarefas assíncronas da aplicação.
     */
    @Bean("backupTaskExecutor")
    public ThreadPoolTaskExecutor backupTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("backup-dump-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
