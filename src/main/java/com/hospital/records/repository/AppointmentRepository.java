package com.hospital.records.repository;

import com.hospital.records.domain.Appointment;
import com.hospital.records.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    /** Two slots overlap when each one starts before the other ends. Back-to-back slots do not overlap. */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.doctor.id = :doctorId
              and a.status = :status
              and a.startTime < :end
              and a.endTime > :start
            """)
    boolean existsOverlap(@Param("doctorId") Long doctorId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("status") AppointmentStatus status);

    /** Same check for rescheduling: the appointment being moved must not conflict with itself. */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.doctor.id = :doctorId
              and a.status = :status
              and a.id <> :excludeId
              and a.startTime < :end
              and a.endTime > :start
            """)
    boolean existsOverlapExcluding(@Param("doctorId") Long doctorId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   @Param("status") AppointmentStatus status,
                                   @Param("excludeId") Long excludeId);

    /** Does this doctor have a (non-cancelled) appointment with this patient? Used for the care-relationship rule. */
    boolean existsByDoctorIdAndPatientIdAndStatusNot(Long doctorId, Long patientId, AppointmentStatus status);
}
