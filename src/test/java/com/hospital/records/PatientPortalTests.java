package com.hospital.records;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Patient logins: self sign-up, booking their own appointments, and everything they must NOT be able to reach. */
@SpringBootTest
@AutoConfigureMockMvc
class PatientPortalTests {

    private static final String AUTH = "Authorization";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final String PASSWORD = "Password@123";
    private static final String DOB = "1990-05-17";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private record Person(long id, String email, String bearer) {
    }

    private record PatientRecord(long id, String mrn) {
    }

    // ---------- helpers ----------

    private JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + body(result).get("token").asText();
    }

    private Person createStaff(String adminBearer, String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@hospital.test";
        String specialization = role.equals("DOCTOR") ? ",\"specialization\":\"Cardiology\"" : "";
        MvcResult result = mvc.perform(post("/api/admin/staff").header(AUTH, adminBearer).contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"Test %s\",\"role\":\"%s\"%s}"
                                .formatted(email, PASSWORD, role, role, specialization)))
                .andExpect(status().isCreated())
                .andReturn();
        return new Person(body(result).get("id").asLong(), email, login(email, PASSWORD));
    }

    private PatientRecord createPatient(String bearer) throws Exception {
        MvcResult result = mvc.perform(post("/api/patients").header(AUTH, bearer).contentType(JSON)
                        .content("""
                                {"fullName":"Portal Patient","dateOfBirth":"%s","gender":"FEMALE","phone":"9876543210"}
                                """.formatted(DOB)))
                .andExpect(status().isCreated())
                .andReturn();
        return new PatientRecord(body(result).get("id").asLong(), body(result).get("mrn").asText());
    }

    private ResultActions register(String email, String mrn, String dob) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"mrn\":\"%s\",\"dateOfBirth\":\"%s\"}"
                        .formatted(email, PASSWORD, mrn, dob)));
    }

    /** Creates a patient record (as reception) and signs that patient up for a login. */
    private Person signedUpPatient(String receptionBearer) throws Exception {
        PatientRecord record = createPatient(receptionBearer);
        String email = "patient-" + UUID.randomUUID() + "@mail.test";
        register(email, record.mrn(), DOB).andExpect(status().isCreated());
        return new Person(record.id(), email, login(email, PASSWORD));
    }

    private static LocalDateTime slot(int daysAhead, int hour) {
        return LocalDateTime.now().plusDays(daysAhead).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    private ResultActions book(String bearer, long doctorId, LocalDateTime start) throws Exception {
        return mvc.perform(post("/api/me/appointments").header(AUTH, bearer).contentType(JSON)
                .content("{\"doctorId\":%d,\"startTime\":\"%s\",\"reason\":\"Fever\"}".formatted(doctorId, start)));
    }

    // ---------- tests ----------

    @Test
    void patientSignsUpWithMrnAndDateOfBirth() throws Exception {
        String admin = login("admin@hospital.test", "Admin@12345");
        Person reception = createStaff(admin, "RECEPTIONIST");
        PatientRecord record = createPatient(reception.bearer());

        String email = "patient-" + UUID.randomUUID() + "@mail.test";

        // wrong date of birth, unknown MRN: refused, with the same message
        register(email, record.mrn(), "1991-01-01").andExpect(status().isBadRequest());
        register(email, "MRN-NOSUCH00", DOB).andExpect(status().isBadRequest());

        // correct details: an account is created, linked to the patient, and can log in
        register(email, record.mrn().toLowerCase(), DOB)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.patientId").value(record.id()));
        String bearer = login(email, PASSWORD);
        mvc.perform(get("/api/auth/me").header(AUTH, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(record.id()));

        // one login per patient, and emails stay unique
        register("other-" + UUID.randomUUID() + "@mail.test", record.mrn(), DOB).andExpect(status().isBadRequest());
        PatientRecord second = createPatient(reception.bearer());
        register(email, second.mrn(), DOB).andExpect(status().isBadRequest());

        // patient logins are not staff: not in the staff list, and an admin cannot create one as staff
        mvc.perform(get("/api/admin/staff").header(AUTH, admin).param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.role=='PATIENT')]").isEmpty());
        mvc.perform(post("/api/admin/staff").header(AUTH, admin).contentType(JSON)
                        .content("{\"email\":\"x-%s@hospital.test\",\"password\":\"%s\",\"fullName\":\"X\",\"role\":\"PATIENT\"}"
                                .formatted(UUID.randomUUID(), PASSWORD)))
                .andExpect(status().isBadRequest());

        // failed sign-ups are audited
        mvc.perform(get("/api/admin/audit").header(AUTH, admin).param("action", "REGISTER_FAILED").param("actorEmail", email))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void patientBooksAndCancelsOnlyOwnAppointments() throws Exception {
        String admin = login("admin@hospital.test", "Admin@12345");
        Person reception = createStaff(admin, "RECEPTIONIST");
        Person doctor = createStaff(admin, "DOCTOR");
        Person alice = signedUpPatient(reception.bearer());
        Person bob = signedUpPatient(reception.bearer());

        // booking: the patient comes from the login, the doctor is checked for conflicts
        MvcResult booked = book(alice.bearer(), doctor.id(), slot(20, 10))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(alice.id()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();
        long aliceAppointment = body(booked).get("id").asLong();

        book(bob.bearer(), doctor.id(), slot(20, 10)).andExpect(status().isConflict());   // same slot, same doctor
        book(bob.bearer(), doctor.id(), slot(-2, 10)).andExpect(status().isBadRequest()); // in the past
        book(bob.bearer(), doctor.id(), slot(20, 11)).andExpect(status().isCreated());

        // each patient sees only their own appointments
        mvc.perform(get("/api/me/appointments").header(AUTH, alice.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(aliceAppointment));
        mvc.perform(get("/api/me/appointments").header(AUTH, bob.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1));

        // bob cannot cancel alice's appointment; it looks like it does not exist
        mvc.perform(post("/api/me/appointments/" + aliceAppointment + "/cancel").header(AUTH, bob.bearer()))
                .andExpect(status().isNotFound());

        // alice cancels her own, and the slot becomes free again
        mvc.perform(post("/api/me/appointments/" + aliceAppointment + "/cancel").header(AUTH, alice.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/me/appointments/" + aliceAppointment + "/cancel").header(AUTH, alice.bearer()))
                .andExpect(status().isConflict());
        book(bob.bearer(), doctor.id(), slot(20, 10)).andExpect(status().isCreated());

        // the front desk sees patient bookings too, and the audit log names the patient
        mvc.perform(get("/api/appointments").header(AUTH, reception.bearer()).param("patientId", String.valueOf(alice.id())))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/admin/audit").header(AUTH, admin).param("actorEmail", alice.email()).param("action", "CANCEL"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // patients can pick a doctor
        mvc.perform(get("/api/doctors").header(AUTH, alice.bearer())).andExpect(status().isOk());
    }

    @Test
    void patientCannotReachAnythingElse() throws Exception {
        String admin = login("admin@hospital.test", "Admin@12345");
        Person reception = createStaff(admin, "RECEPTIONIST");
        Person doctor = createStaff(admin, "DOCTOR");
        Person alice = signedUpPatient(reception.bearer());
        PatientRecord other = createPatient(reception.bearer());

        // the doctor writes a clinical record for another patient
        mvc.perform(post("/api/appointments").header(AUTH, reception.bearer()).contentType(JSON)
                        .content("{\"patientId\":%d,\"doctorId\":%d,\"startTime\":\"%s\"}".formatted(other.id(), doctor.id(), slot(21, 9))))
                .andExpect(status().isCreated());
        MvcResult record = mvc.perform(post("/api/patients/" + other.id() + "/records").header(AUTH, doctor.bearer())
                        .contentType(JSON).content("{\"diagnosis\":\"Hypertension\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long recordId = body(record).get("id").asLong();

        // no patient registry, no clinical records (hers or anyone's), no staff, no audit, no staff-side appointments
        mvc.perform(get("/api/patients").header(AUTH, alice.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/patients/" + other.id()).header(AUTH, alice.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/patients/" + alice.id() + "/records").header(AUTH, alice.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/records/" + recordId).header(AUTH, alice.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/records/" + recordId + "/history").header(AUTH, alice.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/staff").header(AUTH, alice.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").header(AUTH, alice.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/appointments").header(AUTH, alice.bearer())).andExpect(status().isForbidden());
        mvc.perform(post("/api/appointments").header(AUTH, alice.bearer()).contentType(JSON)
                        .content("{\"patientId\":%d,\"doctorId\":%d,\"startTime\":\"%s\"}".formatted(other.id(), doctor.id(), slot(22, 9))))
                .andExpect(status().isForbidden());

        // and staff cannot use the patient area
        mvc.perform(get("/api/me/appointments").header(AUTH, reception.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/me/appointments")).andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatingThePatientRecordBlocksTheLogin() throws Exception {
        String admin = login("admin@hospital.test", "Admin@12345");
        Person reception = createStaff(admin, "RECEPTIONIST");
        Person alice = signedUpPatient(reception.bearer());

        mvc.perform(get("/api/me/appointments").header(AUTH, alice.bearer())).andExpect(status().isOk());

        mvc.perform(patch("/api/patients/" + alice.id() + "/active").header(AUTH, admin)
                        .contentType(JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/me/appointments").header(AUTH, alice.bearer())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(alice.email(), PASSWORD)))
                .andExpect(status().isUnauthorized());
    }
}
