package br.edu.ifrn.promptuario.shared.messaging;

import br.edu.ifrn.promptuario.modules.clinical.model.Appointment;
import br.edu.ifrn.promptuario.modules.clinical.model.AppointmentStatus;
import br.edu.ifrn.promptuario.modules.clinical.repository.AppointmentRepository;
import br.edu.ifrn.promptuario.modules.patient.model.Patient;
import br.edu.ifrn.promptuario.modules.patient.repository.PatientRepository;
import br.edu.ifrn.promptuario.shared.event.UserCreatedEvent;
import br.edu.ifrn.promptuario.shared.event.UserDeactivatedEvent;
import br.edu.ifrn.promptuario.shared.model.AuditLog;
import br.edu.ifrn.promptuario.shared.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static br.edu.ifrn.promptuario.config.RabbitMQConfig.IAM_EVENTS_QUEUE;

@Component
public class IamEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(IamEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    public IamEventConsumer(ObjectMapper objectMapper, PatientRepository patientRepository,
                            AppointmentRepository appointmentRepository, AuditLogRepository auditLogRepository) {
        this.objectMapper = objectMapper;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @RabbitListener(queues = IAM_EVENTS_QUEUE)
    @Transactional
    public void consume(Message message) {
        try {
            var body = objectMapper.readTree(message.getBody());
            String eventId = body.path("eventId").asText(body.path("event_id").asText(null));
            if (eventId != null && processedEvents.contains(eventId)) {
                log.debug("Ignoring duplicate IAM event: eventId={}", eventId);
                return;
            }
            String eventType = message.getMessageProperties().getReceivedRoutingKey();
            if (eventType == null) {
                eventType = body.path("eventType").asText(body.path("event_type").asText());
            }
            if ("user.created".equals(eventType) || "iam.user.created".equals(eventType)
                    || "UserCreated".equals(eventType)) {
                handleUserCreated(objectMapper.readValue(message.getBody(), UserCreatedEvent.class));
            } else if ("user.deactivated".equals(eventType) || "iam.user.deactivated".equals(eventType)
                    || "UserDeactivated".equals(eventType)) {
                handleUserDeactivated(objectMapper.readValue(message.getBody(), UserDeactivatedEvent.class));
            } else {
                log.debug("Ignoring unsupported IAM event: type={}", eventType);
            }
            if (eventId != null) processedEvents.add(eventId);
        } catch (Exception ex) {
            log.error("IAM event processing failed", ex);
            throw new IllegalStateException("IAM event processing failed", ex);
        }
    }

    private void handleUserCreated(UserCreatedEvent event) {
        if (!"PATIENT".equalsIgnoreCase(event.role())) return;
        patientRepository.findByUserId(event.userId()).orElseGet(() -> {
            Patient patient = new Patient();
            patient.setUserId(event.userId());
            patient.setFullName(event.fullName());
            patient.setCpf(event.cpf());
            patient.setPhone(event.phone());
            patient.setEmail(event.email());
            return patientRepository.save(patient);
        });
    }

    private void handleUserDeactivated(UserDeactivatedEvent event) {
        patientRepository.findByUserId(event.userId()).ifPresent(patient -> {
            patient.setActive(false);
            patientRepository.save(patient);

            var statuses = EnumSet.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);
            for (Appointment appointment : appointmentRepository
                    .findByPatient_IdAndScheduledAtAfterAndStatusIn(patient.getId(), Instant.now(), statuses)) {
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointment.setCancellationReason("Usuário desativado no IAM");
                appointment.setCancelledBy(event.userId());
                appointment.setCancelledAt(Instant.now());
                appointmentRepository.save(appointment);
            }
            auditLogRepository.save(AuditLog.create("patient-clinical-service", "patients", "USER_DEACTIVATED",
                    patient.getId(), event.userId(), null,
                    java.util.Map.of("reason", event.reason(), "cancelledAppointments", true)));
        });
    }

}
