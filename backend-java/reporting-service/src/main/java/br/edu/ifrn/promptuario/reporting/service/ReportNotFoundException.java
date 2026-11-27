package br.edu.ifrn.promptuario.reporting.service;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(String id) {
        super("Report job not found: " + id);
    }
}
