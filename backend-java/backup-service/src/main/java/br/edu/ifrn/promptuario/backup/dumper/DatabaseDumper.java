package br.edu.ifrn.promptuario.backup.dumper;

import br.edu.ifrn.promptuario.backup.model.DatabaseBackupResult;
import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import br.edu.ifrn.promptuario.backup.model.ExecutionMode;

/**
 * Contrato comum dos dumpers de banco.
 *
 * <p>Implementações <strong>nunca lançam exceção</strong>: qualquer falha é
 * convertida em um {@link DatabaseBackupResult} com status {@code FAILED}, de
 * modo que uma falha isolada jamais interrompa o backup dos demais bancos.
 */
public interface DatabaseDumper {

    /**
     * Executa o dump do alvo informado e devolve o resultado consolidado.
     *
     * @param target alvo a ser dumpado
     * @param mode   modo de execução corrente (para rastreabilidade)
     * @return resultado do dump (nunca {@code null})
     */
    DatabaseBackupResult dump(DatabaseTarget target, ExecutionMode mode);
}
