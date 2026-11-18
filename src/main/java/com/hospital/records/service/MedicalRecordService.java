package com.hospital.records.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.records.domain.AppointmentStatus;
import com.hospital.records.domain.AuditAction;
import com.hospital.records.domain.MedicalRecord;
import com.hospital.records.domain.MedicalRecordVersion;
import com.hospital.records.domain.Patient;
import com.hospital.records.domain.Role;
import com.hospital.records.domain.StaffUser;
import com.hospital.records.dto.MedicalRecordRequest;
import com.hospital.records.dto.MedicalRecordResponse;
import com.hospital.records.dto.PageResponse;
import com.hospital.records.dto.VersionResponse;
import com.hospital.records.exception.BadRequestException;
import com.hospital.records.exception.ResourceNotFoundException;
import com.hospital.records.repository.AppointmentRepository;
import com.hospital.records.repository.MedicalRecordRepository;
import com.hospital.records.repository.MedicalRecordVersionRepository;
import com.hospital.records.repository.PatientRepository;
import com.hospital.records.repository.StaffUserRepository;
import com.hospital.records.security.Actor;
import com.hospital.records.util.SortValidator;

/**
 * Clinical records. The rules, in one place:
 * <ul>
 *   <li>Only a doctor can create a record, and only for a patient they have a (non-cancelled) appointment with.</li>
 *   <li>Any doctor treating the patient can edit it. Every edit needs a reason and keeps the previous content as a version.</li>
 *   <li>A doctor can read records of patients they treat; nurses can read (never write); admins and receptionists
 *       have no clinical access at all (least privilege).</li>
 *   <li>Every read and write is written to the audit log.</li>
 * </ul>
 */
@Service
public class MedicalRecordService {

    private static final Set<String> SORTABLE = Set.of("id", "createdAt", "updatedAt");

    private final MedicalRecordRepository records;
    private final MedicalRecordVersionRepository versions;
    private final PatientRepository patients;
    private final StaffUserRepository users;
    private final AppointmentRepository appointments;
    private final AuditService audit;

    public MedicalRecordService(MedicalRecordRepository records,
                                MedicalRecordVersionRepository versions,
                                PatientRepository patients,
                                StaffUserRepository users,
                                AppointmentRepository appointments,
                                AuditService audit) {
        this.records = records;
        this.versions = versions;
        this.patients = patients;
        this.users = users;
        this.appointments = appointments;
        this.audit = audit;
    }

    @Transactional
    public MedicalRecordResponse create(Actor actor, Long patientId, MedicalRecordRequest request) {
        StaffUser doctor = currentDoctor(actor);
        Patient patient = findPatient(patientId);

        if (!appointments.existsByDoctorIdAndPatientIdAndStatusNot(
                doctor.getId(), patientId, AppointmentStatus.CANCELLED)) {
            throw new AccessDeniedException("You have no appointment with this patient");
        }

        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setDoctor(doctor);
        applyContent(record, request);
        record.setCurrentVersion(1);
        records.save(record);

        versions.save(new MedicalRecordVersion(record, doctor, "Initial entry"));

        audit.record(actor, AuditAction.CREATE, "MedicalRecord", record.getId(), "patientId=" + patientId);
        return MedicalRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public PageResponse<MedicalRecordResponse> listForPatient(Actor actor, Long patientId, Pageable pageable) {
        SortValidator.require(pageable, SORTABLE);
        findPatient(patientId);
        assertCanRead(actor, patientId);

        audit.record(actor, AuditAction.VIEW, "Patient", patientId, "Viewed clinical records");
        return PageResponse.from(records.findByPatientId(patientId, pageable).map(MedicalRecordResponse::from));
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse get(Actor actor, Long id) {
        MedicalRecord record = find(id);
        Long patientId = record.getPatient().getId();
        assertCanRead(actor, patientId);

        audit.record(actor, AuditAction.VIEW, "MedicalRecord", id, "patientId=" + patientId);
        return MedicalRecordResponse.from(record);
    }

    @Transactional
    public MedicalRecordResponse update(Actor actor, Long id, MedicalRecordRequest request) {
        StaffUser doctor = currentDoctor(actor);
        MedicalRecord record = find(id);

        // Any doctor with a care relationship to the patient may edit; the editor is stored on each version.
        assertCanRead(actor, record.getPatient().getId());
        if (request.changeReason() == null || request.changeReason().isBlank()) {
            throw new BadRequestException("changeReason is required when editing a record");
        }

        applyContent(record, request);
        record.setCurrentVersion(record.getCurrentVersion() + 1);
        // Flush now: a concurrent edit surfaces as an optimistic-locking error (HTTP 409) right here.
        records.saveAndFlush(record);

        versions.save(new MedicalRecordVersion(record, doctor, request.changeReason().trim()));

        audit.record(actor, AuditAction.UPDATE, "MedicalRecord", id,
                "patientId=" + record.getPatient().getId() + ", version=" + record.getCurrentVersion());
        return MedicalRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> history(Actor actor, Long id) {
        MedicalRecord record = find(id);
        Long patientId = record.getPatient().getId();
        assertCanRead(actor, patientId);

        audit.record(actor, AuditAction.VIEW, "MedicalRecord", id, "patientId=" + patientId + ", viewed version history");
        return versions.findByMedicalRecordIdOrderByVersionNumberAsc(id).stream()
                .map(VersionResponse::from)
                .toList();
    }

    // ----- helpers -----

    private MedicalRecord find(Long id) {
        return records.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record " + id + " not found"));
    }

    private Patient findPatient(Long id) {
        return patients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient " + id + " not found"));
    }

    private StaffUser currentDoctor(Actor actor) {
        return users.findByEmail(actor.email())
                .filter(u -> u.getRole() == Role.DOCTOR)
                .orElseThrow(() -> new AccessDeniedException("Only doctors can do this"));
    }

    /** Nurses may read any record; a doctor needs a care relationship (an appointment, or having written a record). */
    private void assertCanRead(Actor actor, Long patientId) {
        if (actor.is(Role.NURSE)) {
            return;
        }
        StaffUser doctor = currentDoctor(actor);
        boolean treating = appointments.existsByDoctorIdAndPatientIdAndStatusNot(
                doctor.getId(), patientId, AppointmentStatus.CANCELLED)
                || records.existsByDoctorIdAndPatientId(doctor.getId(), patientId);
        if (!treating) {
            throw new AccessDeniedException("You have no care relationship with this patient");
        }
    }

    private static void applyContent(MedicalRecord record, MedicalRecordRequest r) {
        record.setDiagnosis(r.diagnosis().trim());
        record.setTreatment(blankToNull(r.treatment()));
        record.setPrescription(blankToNull(r.prescription()));
        record.setNotes(blankToNull(r.notes()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
