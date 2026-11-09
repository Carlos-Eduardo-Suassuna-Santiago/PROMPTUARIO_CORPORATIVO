package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.DoctorSchedule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
public record ScheduleCreateDto(@Size(max = 100) String specialty, List<@Valid TimeSlotDto> slots) {
    public DoctorSchedule toEntity(String doctorId) {
        DoctorSchedule e = new DoctorSchedule(); e.setDoctorId(doctorId); e.setSpecialty(specialty); return e;
    }
}
