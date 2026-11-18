package com.hospital.records.service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.records.domain.Appointment;
import com.hospital.records.domain.AppointmentStatus;
import com.hospital.records.domain.AuditAction;
import com.hospital.records.domain.MedicalRecord;
import com.hospital.records.domain.Patient;
import com.hospital.records.domain.Role;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.PatientRequest;
import com.hospital.records.dto.PatientResponse;
import com.hospital.records.exception.ResourceNotFoundException;
import com.hospital.records.repository.AppointmentRepository;
import com.hospital.records.repository.MedicalRecordRepository;
import com.hospital.records.repository.PatientRepository;
import com.hospital.records.repository.StaffUserRepository;
import com.hospital.records.security.Actor;
import com.hospital.records.util.SortValidator;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class PatientService {

    private static final Set<String> SORTABLE = Set.of("id", "fullName", "dateOfBirth", "createdAt");

    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final MedicalRecordRepository records;
    private final StaffUserRepository users;
    private final AuditService audit;

    public PatientService(PatientRepository patients, AppointmentRepository appointments,
                          MedicalRecordRepository records, StaffUserRepository users, AuditService audit) {
        this.patients = patients;
        this.appointments = appointments;
        this.records = records;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public PatientResponse create(Actor actor, PatientRequest request) {
        Patient patient = new Patient();
        patient.setMrn(newMrn());
        apply(patient, request);
        patients.save(patient);

        audit.record(actor, AuditAction.CREATE, "Patient", patient.getId(), null);
        return PatientResponse.from(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponse get(Actor actor, Long id) {
        Patient patient = find(id);
        if (actor.is(Role.DOCTOR) && !isAssignedTo(doctorId(actor), id)) {
            throw new AccessDeniedException("This patient is not assigned to you");
        }
        audit.record(actor, AuditAction.VIEW, "Patient", id, null);
        return PatientResponse.from(patient);
    }

    /**
     * Searches name, MRN and phone. Only active patients unless {@code active} is false.
     * A doctor only sees their own patients: those with a non-cancelled appointment with them,
     * or for whom they have written a clinical record.
     */
    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> search(Actor actor, String q, boolean active, Pageable pageable) {
        SortValidator.require(pageable, SORTABLE);

        Specification<Patient> spec = (root, cq, cb) -> cb.equal(root.get("active"), active);
        if (actor.is(Role.DOCTOR)) {
            Long doctorId = doctorId(actor);
            spec = spec.and((root, cq, cb) -> {
                Subquery<Long> byAppointment = cq.subquery(Long.class);
                Root<Appointment> a = byAppointment.from(Appointment.class);
                byAppointment.select(a.get("id")).where(
                        cb.equal(a.get("patient").get("id"), root.get("id")),
                        cb.equal(a.get("doctor").get("id"), doctorId),
                        cb.notEqual(a.get("status"), AppointmentStatus.CANCELLED));

                Subquery<Long> byRecord = cq.subquery(Long.class);
                Root<MedicalRecord> m = byRecord.from(MedicalRecord.class);
                byRecord.select(m.get("id")).where(
                        cb.equal(m.get("patient").get("id"), root.get("id")),
                        cb.equal(m.get("doctor").get("id"), doctorId));

                return cb.or(cb.exists(byAppointment), cb.exists(byRecord));
            });
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("mrn")), like),
                    cb.like(root.get("phone"), like)));
        }
        return PageResponse.from(patients.findAll(spec, pageable).map(PatientResponse::from));
    }

    @Transactional
    public PatientResponse update(Actor actor, Long id, PatientRequest request) {
        Patient patient = find(id);
        apply(patient, request);
        patients.saveAndFlush(patient);

        audit.record(actor, AuditAction.UPDATE, "Patient", id, null);
        return PatientResponse.from(patient);
    }

    /** Patients are never deleted: deactivating hides them from search and blocks new bookings, history stays intact. */
    @Transactional
    public PatientResponse setActive(Actor actor, Long id, boolean active) {
        Patient patient = find(id);
        patient.setActive(active);
        patients.saveAndFlush(patient);

        audit.record(actor, active ? AuditAction.ACTIVATE : AuditAction.DEACTIVATE, "Patient", id, null);
        return PatientResponse.from(patient);
    }

    private Long doctorId(Actor actor) {
        return users.findByEmail(actor.email())
                .orElseThrow(() -> new AccessDeniedException("Unknown user"))
                .getId();
    }

    private boolean isAssignedTo(Long doctorId, Long patientId) {
        return appointments.existsByDoctorIdAndPatientIdAndStatusNot(doctorId, patientId, AppointmentStatus.CANCELLED)
                || records.existsByDoctorIdAndPatientId(doctorId, patientId);
    }

    private Patient find(Long id) {
        return patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient " + id + " not found"));
    }

    private String newMrn() {
        String mrn;
        do {
            mrn = "MRN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        } while (patients.existsByMrn(mrn));
        return mrn;
    }

    private static void apply(Patient patient, PatientRequest r) {
        patient.setFullName(r.fullName().trim());
        patient.setDateOfBirth(r.dateOfBirth());
        patient.setGender(r.gender());
        patient.setPhone(r.phone().trim());
        patient.setEmail(blankToNull(r.email()));
        patient.setAddress(blankToNull(r.address()));
        patient.setBloodGroup(blankToNull(r.bloodGroup()));
        patient.setEmergencyContactName(blankToNull(r.emergencyContactName()));
        patient.setEmergencyContactPhone(blankToNull(r.emergencyContactPhone()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
