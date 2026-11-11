package br.edu.ifrn.promptuario.backup.model;

/**
 * Resultado consolidado de um ciclo de backup.
 *
 * <ul>
 *   <li>{@link #SUCCESS} — todos os alvos foram dumpados e enviados com êxito.</li>
 *   <li>{@link #PARTIAL} — o ciclo concluiu, mas ao menos um alvo falhou
 *       (os demais foram preservados).</li>
 *   <li>{@link #FAILED} — o ciclo não produziu nenhum artefato válido.</li>
 * </ul>
 */
public enum BackupStatus {

    SUCCESS,

    PARTIAL,

    FAILED
}
