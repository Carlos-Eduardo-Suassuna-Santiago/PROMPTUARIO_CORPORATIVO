package br.edu.ifrn.promptuario.reporting.repository;

import br.edu.ifrn.promptuario.reporting.model.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, String> {

    List<WebhookConfig> findByActiveTrue();
}