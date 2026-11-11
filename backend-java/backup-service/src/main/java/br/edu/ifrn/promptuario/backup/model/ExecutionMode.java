package br.edu.ifrn.promptuario.backup.model;

/**
 * Modo de execução do ciclo de backup, permitindo rastrear a origem da
 * solicitação (agendada, manual, CLI ou restauração).
 */
public enum ExecutionMode {

    /** Disparado pelo agendador interno ({@code promptuario.backup.schedule-cron}). */
    SCHEDULED,

    /** Disparado sob demanda pela API REST operacional. */
    MANUAL,

    /** Disparado via linha de comando ({@code --once}). */
    CLI,

    /** Execução de restauração controlada ({@code --restore}). */
    RESTORE
}
