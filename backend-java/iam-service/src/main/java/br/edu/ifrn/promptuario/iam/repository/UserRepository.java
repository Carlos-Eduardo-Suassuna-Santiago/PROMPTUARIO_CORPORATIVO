package br.edu.ifrn.promptuario.iam.repository;

import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByCpf(String cpf);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    Page<User> findByRoleAndIsActive(Role role, Boolean isActive, Pageable pageable);

    Page<User> findByIsActive(Boolean isActive, Pageable pageable);

    List<User> findByRoleAndIsActiveTrue(Role role);

    @Override
    Page<User> findAll(Pageable pageable);
}
