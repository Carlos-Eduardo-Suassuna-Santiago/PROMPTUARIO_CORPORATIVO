package br.edu.ifrn.promptuario.modules.patient.dto;

import java.util.List;

public record PatientListResponseDto(List<PatientResponseDto> items, long total, int page, int size) {
}
