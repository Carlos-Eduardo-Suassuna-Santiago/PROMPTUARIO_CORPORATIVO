package br.edu.ifrn.promptuario.iam.repository;

import br.edu.ifrn.promptuario.iam.model.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, String> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<OAuthAccount> findByUserId(String userId);
}
