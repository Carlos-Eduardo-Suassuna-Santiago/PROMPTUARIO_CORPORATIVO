package br.edu.ifrn.promptuario.iam.service;

import br.edu.ifrn.promptuario.iam.dto.DoctorListResponseDTO;
import br.edu.ifrn.promptuario.iam.dto.DoctorResponseDTO;
import br.edu.ifrn.promptuario.iam.dto.PatientRegisterDTO;
import br.edu.ifrn.promptuario.iam.dto.UserCreateDTO;
import br.edu.ifrn.promptuario.iam.dto.UserListResponseDTO;
import br.edu.ifrn.promptuario.iam.dto.UserResponseDTO;
import br.edu.ifrn.promptuario.iam.messaging.IamEventPublisher;
import br.edu.ifrn.promptuario.iam.messaging.events.UserCreatedEvent;
import br.edu.ifrn.promptuario.iam.messaging.events.UserDeactivatedEvent;
import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import br.edu.ifrn.promptuario.iam.repository.RefreshTokenRepository;
import br.edu.ifrn.promptuario.iam.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamEventPublisher eventPublisher;

    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            IamEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserResponseDTO createUser(UserCreateDTO dto) {
        User user = createUser(dto.email(), dto.password(), dto.fullName(), dto.role(), dto.cpf());
        return UserResponseDTO.fromEntity(user);
    }

    @Transactional
    public UserResponseDTO registerPatient(PatientRegisterDTO dto) {
        User user = createUser(dto.email(), dto.password(), dto.fullName(), Role.PATIENT, dto.cpf());
        return UserResponseDTO.fromEntity(user);
    }

    @Transactional
    public void deactivateUser(String userId, String reason, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (!user.isActive()) {
            return;
        }

        Instant deactivatedAt = Instant.now();
        user.setActive(false);
        user.setDeactivatedAt(deactivatedAt);
        user.setDeactivationReason(reason);
        refreshTokenRepository.revokeAllByUserId(userId);
        eventPublisher.publishUserDeactivated(new UserDeactivatedEvent(userId, reason, adminId, deactivatedAt));
    }

    @Transactional(readOnly = true)
    public UserListResponseDTO listUsers(int page, int size, Role role, Boolean isActive) {
        int normalizedPage = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(normalizedPage, size);
        Page<User> users;
        if (role != null && isActive != null) {
            users = userRepository.findByRoleAndIsActive(role, isActive, pageable);
        } else if (isActive != null) {
            users = userRepository.findByIsActive(isActive, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return new UserListResponseDTO(
                users.getContent().stream().map(UserResponseDTO::fromEntity).toList(),
                users.getTotalElements(),
                normalizedPage + 1,
                size
        );
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(String id) {
        return userRepository.findById(id)
                .map(UserResponseDTO::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public DoctorListResponseDTO listDoctors() {
        List<DoctorResponseDTO> doctors = userRepository.findByRoleAndIsActiveTrue(Role.DOCTOR).stream()
                .map(user -> new DoctorResponseDTO(user.getId(), user.getFullName(), user.getEmail(), null, null))
                .toList();
        return new DoctorListResponseDTO(doctors, doctors.size());
    }

    private User createUser(String email, String password, String fullName, Role role, String cpf) {
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        String normalizedCpf = normalizeCpf(cpf);
        validateUniqueIdentity(normalizedEmail, normalizedCpf);

        User user = new User(normalizedEmail, passwordEncoder.encode(password), fullName.trim(), role);
        user.setCpf(normalizedCpf);
        user = userRepository.saveAndFlush(user);

        eventPublisher.publishUserCreated(new UserCreatedEvent(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(), user.getCpf(), user.getCreatedAt()
        ));
        return user;
    }

    private void validateUniqueIdentity(String email, String cpf) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("E-mail já está em uso");
        }
        if (cpf != null && userRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("CPF já está em uso");
        }
    }

    private String normalizeCpf(String cpf) {
        return cpf == null || cpf.isBlank() ? null : cpf.trim();
    }
}
