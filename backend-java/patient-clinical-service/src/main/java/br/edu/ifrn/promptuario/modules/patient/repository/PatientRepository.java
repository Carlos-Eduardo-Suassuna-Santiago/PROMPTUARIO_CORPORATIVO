package br.edu.ifrn.promptuario.modules.patient.repository;

import br.edu.ifrn.promptuario.modules.patient.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, String> {

    Optional<Patient> findByUserId(String userId);

    Optional<Patient> findByCpf(String cpf);

    Page<Patient> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    Page<Patient> findByFullNameContainingIgnoreCaseOrCpfContaining(String fullName, String cpf, Pageable pageable);
}
