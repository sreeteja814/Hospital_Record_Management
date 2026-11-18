package com.hospital.records.repository;

import com.hospital.records.domain.MedicalRecordVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordVersionRepository extends JpaRepository<MedicalRecordVersion, Long> {

    List<MedicalRecordVersion> findByMedicalRecordIdOrderByVersionNumberAsc(Long medicalRecordId);
}
