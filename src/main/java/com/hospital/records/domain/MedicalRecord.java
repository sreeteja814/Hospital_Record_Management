package com.hospital.records.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * The CURRENT content of a clinical record. Every change also writes an immutable
 * {@link MedicalRecordVersion} row, so earlier content is never lost.
 */
@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** The authoring doctor: the only person who may edit this record. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private StaffUser doctor;

    @Column(nullable = false, length = 500)
    private String diagnosis;

    @Column(length = 2000)
    private String treatment;

    @Column(length = 2000)
    private String prescription;

    @Column(length = 4000)
    private String notes;

    /** Human-facing version counter: 1 for the first entry, +1 for every edit. */
    @Column(nullable = false)
    private int currentVersion = 1;

    /** Optimistic locking: if two doctors save at the same moment, the second gets a 409 instead of silently winning. */
    @Version
    private Long lockVersion;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public StaffUser getDoctor() {
        return doctor;
    }

    public void setDoctor(StaffUser doctor) {
        this.doctor = doctor;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(int currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
