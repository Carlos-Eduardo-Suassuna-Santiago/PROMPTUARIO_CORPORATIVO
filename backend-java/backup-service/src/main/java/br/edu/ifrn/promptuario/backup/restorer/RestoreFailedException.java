package br.edu.ifrn.promptuario.backup.restorer;

/**
 * Falha controlada durante uma restauração efetivamente executada (erro do
 * processo de restauração, timeout de segurança ou corrupção detectada pela
 * autenticação AES-GCM).
 *
 * <p>As mensagens <strong>nunca</strong> incluem credenciais: a senha é
 * removida dos textos antes de compor a exceção.
 */
public class RestoreFailedException extends RuntimeException {

    public RestoreFailedException(String message) {
        super(message);
    }

    public RestoreFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
