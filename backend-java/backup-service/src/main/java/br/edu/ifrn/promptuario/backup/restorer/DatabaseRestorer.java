package br.edu.ifrn.promptuario.backup.restorer;

import br.edu.ifrn.promptuario.backup.model.DatabaseTarget;
import java.nio.file.Path;

/**
 * Contrato comum dos restauradores de banco.
 *
 * <p>Semântica de retorno, aplicada por todas as implementações:
 * <ul>
 *   <li>{@code true} — restauração concluída com sucesso;</li>
 *   <li>{@code false} — restauração <strong>não executada</strong> por reprovação
 *       de pré-condição (arquivo ausente ou SHA-256 divergente do manifesto).
 *       Nenhuma conexão é aberta nesta situação;</li>
 *   <li>{@link RestoreFailedException} — restauração efetivamente tentada e
 *       falhou (erro do processo, timeout ou corrupção detectada pela tag GCM).</li>
 * </ul>
 */
public interface DatabaseRestorer {

    /**
     * Restaura um artefato criptografado no alvo informado.
     *
     * @param target              alvo lógico de destino
     * @param encryptedBackupFile artefato local (formato {@code [IV][ciphertext+tag]})
     * @param expectedSha256      SHA-256 esperado do arquivo, conforme o manifesto
     *                            ({@code null}/vazio desabilita a checagem prévia)
     * @return {@code true} se a restauração foi concluída
     * @throws RestoreFailedException se a restauração foi tentada e falhou
     */
    boolean restore(DatabaseTarget target, Path encryptedBackupFile, String expectedSha256);
}
