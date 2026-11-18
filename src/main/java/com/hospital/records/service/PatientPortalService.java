package com.hospital.records.service;

import com.hospital.records.domain.AppointmentStatus;
import com.hospital.records.domain.AuditAction;
import com.hospital.records.domain.Patient;
import com.hospital.records.domain.Role;
import com.hospital.records.domain.StaffUser;
import com.hospital.records.dto.AppointmentRequest;
import com.hospital.records.dto.AppointmentResponse;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.RegisterRequest;
import com.hospital.records.dto.SelfBookingRequest;
import com.hospital.records.dto.UserResponse;
import com.hospital.records.exception.BadRequestException;
import com.hospital.records.repository.PatientRepository;
import com.hospital.records.repository.StaffUserRepository;
import com.hospital.records.security.Actor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * What a patient can do: create their own login, and book, list and cancel THEIR OWN appointments.
 * Patients never reach clinical records, other patients or any staff function.
 */
@Service
public class PatientPortalService {

    /** One message for every failure, so the sign-up form cannot be used to find out who is a patient. */
    private static final String SIGNUP_FAILED =
            "We could not create an account with those details. Check your MRN and date of birth, "
                    + "or ask the reception desk for help.";

    private final StaffUserRepository users;
    private final PatientRepository patients;
    private final AppointmentService appointmentService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public PatientPortalService(StaffUserRepository users,
                                PatientRepository patients,
                                AppointmentService appointmentService,
                                PasswordEncoder passwordEncoder,
                                AuditService audit) {
        this.users = users;
        this.patients = patients;
        this.appointmentService = appointmentService;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String mrn = request.mrn().trim().toUpperCase(Locale.ROOT);

        Patient patient = patients.findByMrn(mrn)
                .filter(Patient::isActive)
                .filter(p -> p.getDateOfBirth().equals(request.dateOfBirth()))
                .filter(p -> !users.existsByPatientId(p.getId()))
                .orElse(null);

        if (patient == null || users.existsByEmail(email)) {
            audit.recordAnonymous(email, AuditAction.REGISTER_FAILED, "StaffUser", null, "Patient sign-up refused");
            throw new BadRequestException(SIGNUP_FAILED);
        }

        StaffUser account = new StaffUser();
        account.setEmail(email);
        account.setFullName(patient.getFullName());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(Role.PATIENT);
        account.setPatientId(patient.getId());

        try {
            users.saveAndFlush(account);
        } catch (DataIntegrityViolationException race) {
            // Two sign-ups for the same patient or email at the same moment: the database refused the second.
            audit.recordAnonymous(email, AuditAction.REGISTER_FAILED, "StaffUser", null, "Patient sign-up refused");
            throw new BadRequestException(SIGNUP_FAILED);
        }

        audit.record(new Actor(email, Role.PATIENT), AuditAction.REGISTER, "StaffUser", account.getId(),
                "patientId=" + patient.getId());
        return UserResponse.from(account);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> myAppointments(Actor actor, AppointmentStatus status, Pageable pageable) {
        return appointmentService.search(actor, null, patientIdOf(actor), status, null, pageable);
    }

    @Transactional
    public AppointmentResponse book(Actor actor, SelfBookingRequest request) {
        return appointmentService.schedule(actor, new AppointmentRequest(
                patientIdOf(actor), request.doctorId(), request.startTime(), null, request.reason()));
    }

    @Transactional
    public AppointmentResponse cancel(Actor actor, Long appointmentId) {
        return appointmentService.cancelForPatient(actor, patientIdOf(actor), appointmentId);
    }

    private Long patientIdOf(Actor actor) {
        return users.findByEmail(actor.email())
                .map(StaffUser::getPatientId)
                .orElseThrow(() -> new AccessDeniedException("This account is not linked to a patient"));
    }
}
