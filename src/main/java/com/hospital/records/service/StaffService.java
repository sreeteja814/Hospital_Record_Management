package com.hospital.records.service;

import com.hospital.records.domain.AuditAction;
import com.hospital.records.domain.Role;
import com.hospital.records.domain.StaffUser;
import com.hospital.records.dto.CreateStaffRequest;
import com.hospital.records.dto.DoctorResponse;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.UserResponse;
import com.hospital.records.exception.BadRequestException;
import com.hospital.records.exception.ConflictException;
import com.hospital.records.exception.ResourceNotFoundException;
import com.hospital.records.repository.StaffUserRepository;
import com.hospital.records.security.Actor;
import com.hospital.records.util.SortValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StaffService {

    private static final Set<String> SORTABLE = Set.of("id", "email", "fullName", "role", "createdAt");

    private final StaffUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public StaffService(StaffUserRepository users, PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional
    public UserResponse create(Actor actor, CreateStaffRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        if (request.role() == Role.PATIENT) {
            throw new BadRequestException("Patient accounts are created by patients themselves, not as staff");
        }

        boolean doctor = request.role() == Role.DOCTOR;
        if (doctor && (request.specialization() == null || request.specialization().isBlank())) {
            throw new BadRequestException("specialization is required for doctors");
        }

        StaffUser user = new StaffUser();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setSpecialization(doctor ? request.specialization().trim() : null);
        users.save(user);

        audit.record(actor, AuditAction.CREATE, "StaffUser", user.getId(), "role=" + user.getRole());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Role role, Pageable pageable) {
        SortValidator.require(pageable, SORTABLE);

        // Patient logins are not staff and never appear in the staff list.
        Specification<StaffUser> spec = (root, cq, cb) -> cb.notEqual(root.get("role"), Role.PATIENT);
        if (role != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("role"), role));
        }
        return PageResponse.from(users.findAll(spec, pageable).map(UserResponse::from));
    }

    @Transactional
    public UserResponse setActive(Actor actor, Long id, boolean active) {
        StaffUser user = users.findById(id)
                .filter(u -> u.getRole() != Role.PATIENT)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user " + id + " not found"));

        if (!active && user.getEmail().equals(actor.email())) {
            throw new BadRequestException("You cannot deactivate your own account");
        }

        user.setActive(active);
        users.save(user);

        audit.record(actor, active ? AuditAction.ACTIVATE : AuditAction.DEACTIVATE, "StaffUser", user.getId(), null);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> activeDoctors() {
        return users.findByRoleAndActiveTrueOrderByFullNameAsc(Role.DOCTOR).stream()
                .map(DoctorResponse::from)
                .toList();
    }
}
