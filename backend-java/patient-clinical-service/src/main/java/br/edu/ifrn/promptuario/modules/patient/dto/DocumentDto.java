package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.DocumentType;

public record DocumentDto(DocumentType documentType, String fileName, long fileSize, String mimeType,
                          String fileHash, String description) {
}
