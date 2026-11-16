package br.edu.ifrn.promptuario.reporting.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DoctorProductivityDTO(
        String doctorId,
        String doctorName,
        long totalConsultations,
        double averagePerDay) {
}