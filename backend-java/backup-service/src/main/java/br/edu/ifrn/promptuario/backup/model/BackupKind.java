package br.edu.ifrn.promptuario.backup.model;

/**
 * Natureza técnica do artefato de backup, usada para selecionar o dumper e a
 * ferramenta de restauração adequada.
 */
public enum BackupKind {

    /** {@code pg_dump} / {@code psql} — bancos PostgreSQL. */
    POSTGRES,

    /** {@code mongodump} / {@code mongorestore} — bancos MongoDB. */
    MONGO
}
