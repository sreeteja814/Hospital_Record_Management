package com.hospital.records.service;

import com.hospital.records.domain.Appointment;
import com.hospital.records.domain.AppointmentStatus;
import com.hospital.records.domain.AuditAction;
import com.hospital.records.domain.Patient;
import com.hospital.records.domain.Role;
import com.hospital.records.domain.StaffUser;
import com.hospital.records.dto.AppointmentRequest;
import com.hospital.records.dto.AppointmentResponse;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.RescheduleRequest;
import com.hospital.records.exception.BadRequestException;
import com.hospital.records.exception.ConflictException;
import com.hospital.records.exception.ResourceNotFoundException;
import com.hospital.records.repository.AppointmentRepository;
import com.hospital.records.repository.PatientRepository;
import com.hospital.records.repository.StaffUserRepository;
import com.hospital.records.security.Actor;
import com.hospital.records.util.SortValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AppointmentService {

    private static final int DEFAULT_MINUTES = 30;
    private static final Set<String> SORTABLE = Set.of("id", "startTime", "status", "createdAt");

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final StaffUserRepository users;
    private final AuditService audit;

    public AppointmentService(AppointmentRepository appointments,
                              PatientRepository patients,
                              StaffUserRepository users,
                              AuditService audit) {
        this.appointments = appointments;
        this.patients = patients;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public AppointmentResponse schedule(Actor actor, AppointmentRequest request) {
        LocalDateTime start = request.startTime();
        LocalDateTime end = endOf(start, request.durationMinutes());

        Patient patient = patients.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient " + request.patientId() + " not found"));
        if (!patient.isActive()) {
            throw new BadRequestException("Patient is deactivated and cannot be booked");
        }

        StaffUser doctor = lockDoctor(request.doctorId());
        if (appointments.existsOverlap(doctor.getId(), start, end, AppointmentStatus.SCHEDULED)) {
            throw new ConflictException("The doctor already has an appointment that overlaps this time slot");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setReason(request.reason() == null || request.reason().isBlank() ? null : request.reason().trim());
        appointments.save(appointment);

        audit.record(actor, AuditAction.CREATE, "Appointment", appointment.getId(),
                "patientId=" + patient.getId() + ", doctorId=" + doctor.getId());
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse reschedule(Actor actor, Long id, RescheduleRequest request) {
        Appointment appointment = find(id);
        requireScheduled(appointment);

        LocalDateTime start = request.startTime();
        LocalDateTime end = endOf(start, request.durationMinutes());

        StaffUser doctor = lockDoctor(appointment.getDoctor().getId());
        if (appointments.existsOverlapExcluding(doctor.getId(), start, end, AppointmentStatus.SCHEDULED, id)) {
            throw new ConflictException("The doctor already has an appointment that overlaps this time slot");
        }

        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointments.saveAndFlush(appointment);

        audit.record(actor, AuditAction.RESCHEDULE, "Appointment", id, null);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(Actor actor, Long id) {
        Appointment appointment = find(id);
        requireAssignedIfDoctor(actor, appointment);
        requireScheduled(appointment);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointments.saveAndFlush(appointment);

        audit.record(actor, AuditAction.CANCEL, "Appointment", id, null);
        return AppointmentResponse.from(appointment);
    }

    /** A patient cancelling their own appointment. Someone else's appointment looks like it does not exist. */
    @Transactional
    public AppointmentResponse cancelForPatient(Actor actor, Long patientId, Long id) {
        Appointment appointment = find(id);
        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new ResourceNotFoundException("Appointment " + id + " not found");
        }
        requireScheduled(appointment);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointments.saveAndFlush(appointment);

        audit.record(actor, AuditAction.CANCEL, "Appointment", id, "cancelled by the patient");
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse complete(Actor actor, Long id) {
        Appointment appointment = find(id);
        requireAssignedIfDoctor(actor, appointment);
        requireScheduled(appointment);

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointments.saveAndFlush(appointment);

        audit.record(actor, AuditAction.COMPLETE, "Appointment", id, null);
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Actor actor, Long id) {
        Appointment appointment = find(id);
        requireAssignedIfDoctor(actor, appointment);
        return AppointmentResponse.from(appointment);
    }

    /** Doctors only ever see their own schedule, whatever filters they send. */
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> search(Actor actor,
                                                    Long doctorId,
                                                    Long patientId,
                                                    AppointmentStatus status,
                                                    LocalDate date,
                                                    Pageable pageable) {
        SortValidator.require(pageable, SORTABLE);

        final Long effectiveDoctorId = actor.is(Role.DOCTOR)
                ? users.findByEmail(actor.email()).map(StaffUser::getId)
                        .orElseThrow(() -> new AccessDeniedException("Unknown doctor"))
                : doctorId;

        Specification<Appointment> spec = (root, cq, cb) -> cb.conjunction();
        if (effectiveDoctorId != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("doctor").get("id"), effectiveDoctorId));
        }
        if (patientId != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (status != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }
        if (date != null) {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.plusDays(1).atStartOfDay();
            spec = spec.and((root, cq, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.<LocalDateTime>get("startTime"), from),
                    cb.lessThan(root.<LocalDateTime>get("startTime"), to)));
        }

        return PageResponse.from(appointments.findAll(spec, pageable).map(AppointmentResponse::from));
    }

    private Appointment find(Long id) {
        return appointments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment " + id + " not found"));
    }

    private LocalDateTime endOf(LocalDateTime start, Integer durationMinutes) {
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Appointment must start in the future");
        }
        return start.plusMinutes(durationMinutes == null ? DEFAULT_MINUTES : durationMinutes);
    }

    /** Locks the doctor's row so concurrent bookings for the same doctor are handled one at a time. */
    private StaffUser lockDoctor(Long doctorId) {
        StaffUser doctor = users.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor " + doctorId + " not found"));
        if (doctor.getRole() != Role.DOCTOR || !doctor.isActive()) {
            throw new ResourceNotFoundException("Doctor " + doctorId + " not found");
        }
        return doctor;
    }

    private static void requireScheduled(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Appointment is already " + appointment.getStatus());
        }
    }

    private static void requireAssignedIfDoctor(Actor actor, Appointment appointment) {
        if (actor.is(Role.DOCTOR) && !appointment.getDoctor().getEmail().equals(actor.email())) {
            throw new AccessDeniedException("This appointment belongs to another doctor");
        }
    }
}
