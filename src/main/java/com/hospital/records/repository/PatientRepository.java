package com.hospital.records.repository;

import com.hospital.records.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    boolean existsByMrn(String mrn);

    Optional<Patient> findByMrn(String mrn);
}
