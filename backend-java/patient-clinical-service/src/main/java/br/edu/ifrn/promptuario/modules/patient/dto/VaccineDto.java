package br.edu.ifrn.promptuario.modules.patient.dto;

import java.time.LocalDate;

public record VaccineDto(String name, String dose, LocalDate appliedAt, LocalDate nextDoseAt, String notes) {
}
