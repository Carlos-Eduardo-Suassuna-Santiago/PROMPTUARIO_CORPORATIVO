package br.edu.ifrn.promptuario.backup.model;

/**
 * Natureza técnica do artefato de backup, usada para selecionar o dumper, a
 * ferramenta de restauração e o diretório/prefixo de chave no MinIO.
 */
public enum BackupKind {

    /** {@code pg_dump} / {@code psql} — bancos PostgreSQL. */
    POSTGRES("postgresql"),

    /** {@code mongodump} / {@code mongorestore} — bancos MongoDB. */
    MONGO("mongodb");

    private final String folder;

    BackupKind(String folder) {
        this.folder = folder;
    }

    /**
     * Nome do diretório local e prefixo da chave no MinIO
     * ({@code backups/{folder}/{dbname}/{yyyy}/{MM}/{dd}/{filename}}).
     * Mantém paridade com o layout legado produzido pelo runner Python.
     */
    public String folder() {
        return folder;
    }
}
