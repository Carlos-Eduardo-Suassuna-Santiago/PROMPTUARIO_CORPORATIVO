package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.TimeSlot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
public record TimeSlotDto(String id, String scheduleId, @NotNull LocalDate slotDate,
                          @NotBlank @Pattern(regexp = "^\\d{2}:\\d{2}$") String startTime,
                          @NotBlank @Pattern(regexp = "^\\d{2}:\\d{2}$") String endTime,
                          boolean available) {
    public static TimeSlotDto fromEntity(TimeSlot e) {
        return new TimeSlotDto(e.getId(), e.getSchedule().getId(), e.getSlotDate(), e.getStartTime(), e.getEndTime(), e.isAvailable());
    }
}
