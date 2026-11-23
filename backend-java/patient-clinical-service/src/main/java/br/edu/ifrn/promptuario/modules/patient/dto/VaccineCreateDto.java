package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.Vaccine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.LocalDate;

public record VaccineCreateDto(@NotBlank @Size(min = 2, max = 255) String name, String dose,
                               String manufacturer,
                               @JsonAlias({"lot_number", "lotNumber"}) String lotNumber,
                               @JsonAlias({"applied_at", "appliedAt"}) LocalDate appliedAt,
                               @JsonAlias({"next_dose_at", "nextDoseAt"}) LocalDate nextDoseAt, String notes) {
    public Vaccine toEntity() {
        Vaccine e = new Vaccine(); e.setName(name); e.setDose(dose); e.setManufacturer(manufacturer);
        e.setLotNumber(lotNumber); e.setAppliedAt(appliedAt); e.setNextDoseAt(nextDoseAt); e.setNotes(notes); return e;
    }
}
