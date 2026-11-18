package com.hospital.records.repository;

import com.hospital.records.domain.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Page<MedicalRecord> findByPatientId(Long patientId, Pageable pageable);

    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);
}
