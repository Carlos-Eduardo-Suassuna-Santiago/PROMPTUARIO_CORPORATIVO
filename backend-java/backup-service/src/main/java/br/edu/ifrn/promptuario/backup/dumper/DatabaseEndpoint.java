package br.edu.ifrn.promptuario.backup.dumper;

import br.edu.ifrn.promptuario.backup.model.BackupKind;

/**
 * Credenciais e localização de um alvo de dump.
 *
 * @param host     hostname do servidor de banco
 * @param port     porta TCP
 * @param user     usuário de conexão
 * @param password senha (nunca deve ser registrada em log)
 * @param dbname   nome do banco
 */
public record DatabaseEndpoint(String host, int port, String user, String password, String dbname) {
}
