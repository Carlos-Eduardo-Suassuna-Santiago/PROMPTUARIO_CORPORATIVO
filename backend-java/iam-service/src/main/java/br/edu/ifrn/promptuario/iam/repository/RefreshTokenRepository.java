package br.edu.ifrn.promptuario.iam.repository;

import br.edu.ifrn.promptuario.iam.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Transactional
    @Query("""
            update RefreshToken token
               set token.revoked = true
             where token.user.id = :userId
               and token.revoked = false
            """)
    int revokeAllByUserId(@Param("userId") String userId);
}
