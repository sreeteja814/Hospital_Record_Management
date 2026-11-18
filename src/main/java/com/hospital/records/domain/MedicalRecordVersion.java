package com.hospital.records.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/** An immutable snapshot of a medical record as it was after one particular edit. Never updated or deleted. */
@Entity
@Table(name = "medical_record_versions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"medical_record_id", "versionNumber"}))
public class MedicalRecordVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_record_id", nullable = false, updatable = false)
    private MedicalRecord medicalRecord;

    @Column(nullable = false, updatable = false)
    private int versionNumber;

    @Column(nullable = false, updatable = false, length = 500)
    private String diagnosis;

    @Column(updatable = false, length = 2000)
    private String treatment;

    @Column(updatable = false, length = 2000)
    private String prescription;

    @Column(updatable = false, length = 4000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edited_by_id", nullable = false, updatable = false)
    private StaffUser editedBy;

    @Column(nullable = false, updatable = false)
    private Instant editedAt;

    @Column(nullable = false, updatable = false, length = 255)
    private String changeReason;

    protected MedicalRecordVersion() {
        // for JPA
    }

    public MedicalRecordVersion(MedicalRecord medicalRecord, StaffUser editedBy, String changeReason) {
        this.medicalRecord = medicalRecord;
        this.versionNumber = medicalRecord.getCurrentVersion();
        this.diagnosis = medicalRecord.getDiagnosis();
        this.treatment = medicalRecord.getTreatment();
        this.prescription = medicalRecord.getPrescription();
        this.notes = medicalRecord.getNotes();
        this.editedBy = editedBy;
        this.editedAt = Instant.now();
        this.changeReason = changeReason;
    }

    public Long getId() {
        return id;
    }

    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public String getPrescription() {
        return prescription;
    }

    public String getNotes() {
        return notes;
    }

    public StaffUser getEditedBy() {
        return editedBy;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public String getChangeReason() {
        return changeReason;
    }
}
