package com.hospital.records.config;

import com.hospital.records.domain.Appointment;
import com.hospital.records.domain.Gender;
import com.hospital.records.domain.Patient;
import com.hospital.records.domain.Role;
import com.hospital.records.domain.StaffUser;
import com.hospital.records.repository.AppointmentRepository;
import com.hospital.records.repository.PatientRepository;
import com.hospital.records.repository.StaffUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Runs once at startup: creates the first administrator (there is no public sign-up, so someone has to exist),
 * and optionally some FICTIONAL demo staff and patients so the API can be tried straight away.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StaffUserRepository users;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final PasswordEncoder passwordEncoder;

    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;
    private final boolean seedDemo;
    private final String demoPassword;

    public DataSeeder(StaffUserRepository users,
                      PatientRepository patients,
                      AppointmentRepository appointments,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.bootstrap.admin-email}") String adminEmail,
                      @Value("${app.bootstrap.admin-password}") String adminPassword,
                      @Value("${app.bootstrap.admin-name}") String adminName,
                      @Value("${app.seed.demo-data}") boolean seedDemo,
                      @Value("${app.seed.demo-password}") String demoPassword) {
        this.users = users;
        this.patients = patients;
        this.appointments = appointments;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
        this.seedDemo = seedDemo;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByRole(Role.ADMIN)) {
            createStaff(adminEmail, adminName, Role.ADMIN, null, adminPassword);
            log.info("Created the first administrator: {}", adminEmail);
        }

        if (seedDemo && patients.count() == 0) {
            seedDemoData();
        }
    }

    private void seedDemoData() {
        StaffUser drRao = createStaff("dr.rao@hospital.test", "Dr. Meera Rao", Role.DOCTOR, "Cardiology", demoPassword);
        createStaff("dr.iyer@hospital.test", "Dr. Arjun Iyer", Role.DOCTOR, "General Medicine", demoPassword);
        createStaff("nurse.priya@hospital.test", "Priya Nair", Role.NURSE, null, demoPassword);
        createStaff("reception@hospital.test", "Kavita Menon", Role.RECEPTIONIST, null, demoPassword);

        Patient asha = createPatient("Asha Verma", LocalDate.of(1988, 3, 14), Gender.FEMALE, "9000000001", "B+");
        createPatientLogin("asha.verma@patient.test", asha);
        createPatient("Ravi Kumar", LocalDate.of(1975, 11, 2), Gender.MALE, "9000000002", "O+");
        createPatient("Sneha Reddy", LocalDate.of(2001, 7, 25), Gender.FEMALE, "9000000003", "A-");

        LocalDateTime tomorrowTenAm = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Appointment appointment = new Appointment();
        appointment.setPatient(asha);
        appointment.setDoctor(drRao);
        appointment.setStartTime(tomorrowTenAm);
        appointment.setEndTime(tomorrowTenAm.plusMinutes(30));
        appointment.setReason("Routine cardiology check-up");
        appointments.save(appointment);

        log.info("Seeded fictional demo data (staff accounts use the password from app.seed.demo-password). "
                + "Set SEED_DEMO_DATA=false to disable.");
    }

    private StaffUser createStaff(String email, String name, Role role, String specialization, String password) {
        StaffUser user = new StaffUser();
        user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setFullName(name);
        user.setRole(role);
        user.setSpecialization(specialization);
        user.setPasswordHash(passwordEncoder.encode(password));
        return users.save(user);
    }

    private void createPatientLogin(String email, Patient patient) {
        StaffUser account = createStaff(email, patient.getFullName(), Role.PATIENT, null, demoPassword);
        account.setPatientId(patient.getId());
        users.save(account);
    }

    private Patient createPatient(String name, LocalDate dob, Gender gender, String phone, String bloodGroup) {
        Patient patient = new Patient();
        patient.setMrn("MRN-DEMO" + String.format("%04d", patients.count() + 1));
        patient.setFullName(name);
        patient.setDateOfBirth(dob);
        patient.setGender(gender);
        patient.setPhone(phone);
        patient.setBloodGroup(bloodGroup);
        return patients.save(patient);
    }
}
