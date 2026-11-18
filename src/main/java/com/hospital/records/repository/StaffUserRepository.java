package com.hospital.records.repository;

import com.hospital.records.domain.Role;
import com.hospital.records.domain.StaffUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long>, JpaSpecificationExecutor<StaffUser> {

    Optional<StaffUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    boolean existsByPatientId(Long patientId);

    List<StaffUser> findByRoleAndActiveTrueOrderByFullNameAsc(Role role);

    /**
     * Locks the row until the surrounding transaction ends. Booking a doctor takes this lock first, so two
     * receptionists booking the same doctor at the same moment are processed one after the other and the
     * overlap check cannot be fooled by a race.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from StaffUser u where u.id = :id")
    Optional<StaffUser> findByIdForUpdate(@Param("id") Long id);
}
