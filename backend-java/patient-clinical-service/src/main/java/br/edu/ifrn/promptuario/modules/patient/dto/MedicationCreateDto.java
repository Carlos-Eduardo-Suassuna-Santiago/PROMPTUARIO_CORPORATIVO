package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.ContinuousMedication;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDate;

public record MedicationCreateDto(@NotBlank @Size(min = 2, max = 255) String name,
                                  @NotBlank @Size(max = 100) String dosage,
                                  @NotBlank @Size(max = 100) String frequency,
                                  String route,
                                  @JsonAlias({"prescribing_doctor", "prescribingDoctor"}) String prescribingDoctor,
                                  @JsonAlias({"started_at", "startedAt"}) LocalDate startedAt, String notes) {
    public ContinuousMedication toEntity() {
        ContinuousMedication e = new ContinuousMedication(); e.setName(name); e.setDosage(dosage);
        e.setFrequency(frequency); e.setRoute(route); e.setPrescribingDoctor(prescribingDoctor);
        e.setStartedAt(startedAt); e.setNotes(notes); return e;
    }
}
