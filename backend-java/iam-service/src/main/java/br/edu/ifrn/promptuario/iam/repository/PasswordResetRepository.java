package br.edu.ifrn.promptuario.iam.repository;

import br.edu.ifrn.promptuario.iam.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);
}
