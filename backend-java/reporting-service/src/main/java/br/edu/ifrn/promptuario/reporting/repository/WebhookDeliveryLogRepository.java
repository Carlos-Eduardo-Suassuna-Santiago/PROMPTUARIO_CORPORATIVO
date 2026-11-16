package br.edu.ifrn.promptuario.reporting.repository;

import br.edu.ifrn.promptuario.reporting.model.WebhookDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, String> {

    List<WebhookDeliveryLog> findByJobIdOrderByDeliveredAtDesc(String jobId);
}