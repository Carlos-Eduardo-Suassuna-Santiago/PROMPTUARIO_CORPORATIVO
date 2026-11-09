package br.edu.ifrn.promptuario.modules.clinical.controller;

import br.edu.ifrn.promptuario.modules.clinical.dto.*;
import br.edu.ifrn.promptuario.modules.clinical.model.*;
import br.edu.ifrn.promptuario.modules.clinical.repository.*;
import br.edu.ifrn.promptuario.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController @RequestMapping("/api/v1/schedules") @Tag(name="Doctor schedules")
public class DoctorScheduleController {
    private final DoctorScheduleRepository schedules; private final TimeSlotRepository slots;
    public DoctorScheduleController(DoctorScheduleRepository schedules,TimeSlotRepository slots){this.schedules=schedules;this.slots=slots;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')") @Operation(summary="Create doctor availability")
    public List<TimeSlotDto> create(@Valid @RequestBody ScheduleCreateDto dto,Authentication auth){DoctorSchedule s=dto.toEntity(auth.getName());if(dto.slots()!=null)for(TimeSlotDto d:dto.slots()){TimeSlot t=new TimeSlot();t.setSchedule(s);t.setSlotDate(d.slotDate());t.setStartTime(d.startTime());t.setEndTime(d.endTime());t.setAvailable(d.available());s.getSlots().add(t);}DoctorSchedule saved=schedules.save(s);return saved.getSlots().stream().map(TimeSlotDto::fromEntity).toList();}
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT')") public List<Map<String,Object>> list(Authentication auth){return schedules.findByDoctorId(auth.getName()).stream().map(s -> {Map<String,Object> response=new LinkedHashMap<>();response.put("id",s.getId());response.put("doctorId",s.getDoctorId());response.put("specialty",s.getSpecialty());response.put("active",s.isActive());response.put("createdAt",s.getCreatedAt());response.put("slots",s.getSlots().stream().map(TimeSlotDto::fromEntity).toList());return response;}).toList();}
    @GetMapping("/{id}/available-slots") @PreAuthorize("permitAll()") @Operation(summary="List available slots") public List<TimeSlotDto> available(@PathVariable String id,@RequestParam(required=false) LocalDate date){DoctorSchedule s=schedules.findById(id).orElseThrow(()->new ResourceNotFoundException("Grade não encontrada"));List<TimeSlot> result=date==null?s.getSlots():s.getSlots().stream().filter(x->date.equals(x.getSlotDate())).toList();return result.stream().filter(TimeSlot::isAvailable).map(TimeSlotDto::fromEntity).toList();}
}
