package br.edu.ifrn.promptuario.modules.patient.controller;

import br.edu.ifrn.promptuario.modules.patient.dto.*;
import br.edu.ifrn.promptuario.modules.patient.model.*;
import br.edu.ifrn.promptuario.modules.patient.repository.PatientDocumentRepository;
import br.edu.ifrn.promptuario.modules.patient.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients")
@Transactional
public class PatientController {
    private final PatientService patients; private final AllergyService allergies; private final VaccineService vaccines;
    private final MedicationService medications; private final PatientDocumentService documents; private final PatientDocumentRepository documentRepository;
    public PatientController(PatientService patients, AllergyService allergies, VaccineService vaccines, MedicationService medications, PatientDocumentService documents, PatientDocumentRepository documentRepository) {
        this.patients=patients; this.allergies=allergies; this.vaccines=vaccines; this.medications=medications; this.documents=documents; this.documentRepository=documentRepository;
    }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT')") @Operation(summary="List patients")
    public PatientListResponseDto list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String search){return patients.listPatients(page,size,search);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','ATTENDANT')") @Operation(summary="Create patient")
    public PatientResponseDto create(@Valid @RequestBody PatientCreateDto dto, Authentication auth){return patients.createPatient(dto, auth.getName());}
    @GetMapping("/me") @PreAuthorize("hasRole('PATIENT')") public PatientResponseDto me(Authentication auth){return PatientResponseDto.fromEntity(patients.getPatientByUserId(auth.getName()));}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT') or #id == authentication.principal.claims['patient_id']") public PatientResponseDto get(@PathVariable String id){return PatientResponseDto.fromEntity(patients.getPatientById(id));}
    @PatchMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','ATTENDANT') or #id == authentication.principal.claims['patient_id']") public PatientResponseDto update(@PathVariable String id,@Valid @RequestBody PatientUpdateDto dto,Authentication auth){return patients.updatePatient(id,dto,auth.getName());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") public void anonymize(@PathVariable String id,Authentication auth){patients.anonymizePatient(id,auth.getName());}
    @PostMapping("/{id}/allergies") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT')") public AllergyResponseDto addAllergy(@PathVariable String id,@Valid @RequestBody AllergyCreateDto dto){return AllergyResponseDto.fromEntity(allergies.addAllergy(id,dto));}
    @GetMapping("/{id}/allergies") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public List<AllergyResponseDto> listAllergies(@PathVariable String id){return allergies.listAllergies(id).stream().map(AllergyResponseDto::fromEntity).toList();}
    @PostMapping("/{id}/vaccines") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT')") public VaccineResponseDto addVaccine(@PathVariable String id,@Valid @RequestBody VaccineCreateDto dto,Authentication auth){return VaccineResponseDto.fromEntity(vaccines.addVaccine(id,dto,auth.getName()));}
    @GetMapping("/{id}/vaccines") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public List<VaccineResponseDto> listVaccines(@PathVariable String id){return vaccines.listVaccines(id).stream().map(VaccineResponseDto::fromEntity).toList();}
    @PostMapping("/{id}/medications") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')") public MedicationResponseDto addMedication(@PathVariable String id,@Valid @RequestBody MedicationCreateDto dto,Authentication auth){return MedicationResponseDto.fromEntity(medications.addMedication(id,dto,auth.getName()));}
    @GetMapping("/{id}/medications") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public List<MedicationResponseDto> listMedications(@PathVariable String id,@RequestParam(defaultValue="true") boolean activeOnly){return medications.listMedications(id,activeOnly).stream().map(MedicationResponseDto::fromEntity).toList();}
    @PostMapping(value="/{id}/documents/upload", consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT')") public DocumentResponseDto upload(@PathVariable String id,@RequestPart MultipartFile file,@RequestParam(required=false) String title,@RequestParam(required=false) String category,Authentication auth){return documents.uploadPatientDocument(id,file,title,category,auth.getName());}
    @GetMapping("/{id}/documents") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public List<DocumentResponseDto> listDocuments(@PathVariable String id){patients.getPatientById(id); return documentRepository.findByPatient_IdAndActiveTrueOrderByCreatedAtDesc(id).stream().map(DocumentResponseDto::fromEntity).toList();}
    @GetMapping("/{id}/summary") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public PatientSummaryResponseDto summary(@PathVariable String id){return PatientSummaryResponseDto.fromEntity(patients.getPatientById(id));}
}
