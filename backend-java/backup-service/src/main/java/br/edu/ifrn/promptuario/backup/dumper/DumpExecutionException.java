package br.edu.ifrn.promptuario.backup.dumper;

/**
 * Falha controlada na execução de um processo de dump (código de saída não
 * zero ou timeout). É usada internamente para produzir um
 * {@code DatabaseBackupResult} com status {@code FAILED} sem interromper o
 * ciclo dos demais bancos.
 */
public class DumpExecutionException extends RuntimeException {

    public DumpExecutionException(String message) {
        super(message);
    }

    public DumpExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
