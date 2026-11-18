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

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests: real Spring context, real security filter chain, in-memory H2 database.
 * Every test creates its own staff and patients with random names, so tests never depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HospitalApiTests {

    private static final String AUTH = "Authorization";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    // The bootstrap administrator from application.properties
    private static final String ADMIN_EMAIL = "admin@hospital.test";
    private static final String ADMIN_PASSWORD = "Admin@12345";

    private static final String STAFF_PASSWORD = "Password@123";

    private static final String RECORD_JSON = """
            {"diagnosis":"Hypertension","treatment":"Lifestyle changes","prescription":"Amlodipine 5 mg","notes":"BP 150/95"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private record Staff(long id, String email, String bearer) {
    }

    // ---------- helpers ----------

    private JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + body(result).get("token").asText();
    }

    private String adminBearer() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private Staff createStaff(String adminBearer, String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@hospital.test";
        String specialization = role.equals("DOCTOR") ? ",\"specialization\":\"Cardiology\"" : "";
        MvcResult result = mvc.perform(post("/api/admin/staff")
                        .header(AUTH, adminBearer)
                        .contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"Test %s\",\"role\":\"%s\"%s}"
                                .formatted(email, STAFF_PASSWORD, role, role, specialization)))
                .andExpect(status().isCreated())
                .andReturn();
        return new Staff(body(result).get("id").asLong(), email, login(email, STAFF_PASSWORD));
    }

    private long createPatient(String bearer, String fullName) throws Exception {
        MvcResult result = mvc.perform(post("/api/patients")
                        .header(AUTH, bearer)
                        .contentType(JSON)
                        .content("""
                                {"fullName":"%s","dateOfBirth":"1990-05-17","gender":"FEMALE","phone":"9876543210","bloodGroup":"O+"}
                                """.formatted(fullName)))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asLong();
    }

    private String uniqueToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private static LocalDateTime slot(int daysAhead, int hour) {
        return LocalDateTime.now().plusDays(daysAhead).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    private ResultActions schedule(String bearer, long patientId, long doctorId, LocalDateTime start) throws Exception {
        return mvc.perform(post("/api/appointments")
                .header(AUTH, bearer)
                .contentType(JSON)
                .content("""
                        {"patientId":%d,"doctorId":%d,"startTime":"%s","durationMinutes":30,"reason":"Check-up"}
                        """.formatted(patientId, doctorId, start)));
    }

    private long scheduleOk(String bearer, long patientId, long doctorId, LocalDateTime start) throws Exception {
        MvcResult result = schedule(bearer, patientId, doctorId, start)
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asLong();
    }

    private long createRecord(String doctorBearer, long patientId) throws Exception {
        MvcResult result = mvc.perform(post("/api/patients/" + patientId + "/records")
                        .header(AUTH, doctorBearer)
                        .contentType(JSON)
                        .content(RECORD_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asLong();
    }

    // ---------- authentication ----------

    @Test
    void authenticationBasics() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/patients").header(AUTH, "Bearer not-a-real-token")).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                        .contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                        .contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(get("/api/auth/me").header(AUTH, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ---------- staff management ----------

    @Test
    void onlyAdminsManageStaff() throws Exception {
        String admin = adminBearer();
        Staff receptionist = createStaff(admin, "RECEPTIONIST");

        String newStaffJson = "{\"email\":\"%s@hospital.test\",\"password\":\"%s\",\"fullName\":\"New Nurse\",\"role\":\"NURSE\"}"
                .formatted(uniqueToken(), STAFF_PASSWORD);

        mvc.perform(post("/api/admin/staff").header(AUTH, receptionist.bearer()).contentType(JSON).content(newStaffJson))
                .andExpect(status().isForbidden());

        // duplicate email
        mvc.perform(post("/api/admin/staff").header(AUTH, admin).contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"Copy\",\"role\":\"NURSE\"}"
                                .formatted(receptionist.email(), STAFF_PASSWORD)))
                .andExpect(status().isConflict());

        // a doctor needs a specialization
        mvc.perform(post("/api/admin/staff").header(AUTH, admin).contentType(JSON)
                        .content("{\"email\":\"%s@hospital.test\",\"password\":\"%s\",\"fullName\":\"No Spec\",\"role\":\"DOCTOR\"}"
                                .formatted(uniqueToken(), STAFF_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    // ---------- patients ----------

    @Test
    void receptionistRegistersAndSearchesPatients() throws Exception {
        String admin = adminBearer();
        Staff receptionist = createStaff(admin, "RECEPTIONIST");
        Staff doctor = createStaff(admin, "DOCTOR");

        String token = uniqueToken();
        long id = createPatient(receptionist.bearer(), "Zed " + token);

        mvc.perform(get("/api/patients").header(AUTH, receptionist.bearer()).param("q", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].mrn").value(startsWith("MRN-")));

        mvc.perform(get("/api/patients/" + id).header(AUTH, doctor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Zed " + token));

        // doctors cannot register patients
        mvc.perform(post("/api/patients").header(AUTH, doctor.bearer()).contentType(JSON)
                        .content("{\"fullName\":\"X\",\"dateOfBirth\":\"1990-01-01\",\"gender\":\"MALE\",\"phone\":\"9876543210\"}"))
                .andExpect(status().isForbidden());

        // validation
        mvc.perform(post("/api/patients").header(AUTH, receptionist.bearer()).contentType(JSON)
                        .content("{\"fullName\":\"\",\"dateOfBirth\":\"2999-01-01\",\"gender\":\"MALE\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.dateOfBirth").exists())
                .andExpect(jsonPath("$.errors.phone").exists());

        mvc.perform(put("/api/patients/" + id).header(AUTH, receptionist.bearer()).contentType(JSON)
                        .content("{\"fullName\":\"Zed %s\",\"dateOfBirth\":\"1990-05-17\",\"gender\":\"FEMALE\",\"phone\":\"9123456789\"}"
                                .formatted(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("9123456789"));
    }

    // ---------- appointments ----------

    @Test
    void doctorCannotBeDoubleBooked() throws Exception {
        String admin = adminBearer();
        Staff receptionist = createStaff(admin, "RECEPTIONIST");
        Staff doctor1 = createStaff(admin, "DOCTOR");
        Staff doctor2 = createStaff(admin, "DOCTOR");
        long patient = createPatient(receptionist.bearer(), "Patient " + uniqueToken());

        LocalDateTime nine = slot(10, 9);
        scheduleOk(receptionist.bearer(), patient, doctor1.id(), nine);

        // overlaps the first slot (09:15 to 09:45 vs 09:00 to 09:30)
        schedule(receptionist.bearer(), patient, doctor1.id(), nine.plusMinutes(15))
                .andExpect(status().isConflict());

        // back-to-back is fine
        schedule(receptionist.bearer(), patient, doctor1.id(), nine.plusMinutes(30))
                .andExpect(status().isCreated());

        // same time, different doctor is fine
        schedule(receptionist.bearer(), patient, doctor2.id(), nine)
                .andExpect(status().isCreated());

        // cannot book in the past
        schedule(receptionist.bearer(), patient, doctor1.id(), LocalDateTime.now().minusDays(1))
                .andExpect(status().isBadRequest());

        // doctors cannot book appointments themselves
        schedule(doctor1.bearer(), patient, doctor1.id(), slot(11, 9))
                .andExpect(status().isForbidden());
    }

    @Test
    void rescheduleCancelAndComplete() throws Exception {
        String admin = adminBearer();
        Staff receptionist = createStaff(admin, "RECEPTIONIST");
        Staff doctor = createStaff(admin, "DOCTOR");
        Staff otherDoctor = createStaff(admin, "DOCTOR");
        long patient = createPatient(receptionist.bearer(), "Patient " + uniqueToken());

        long first = scheduleOk(receptionist.bearer(), patient, doctor.id(), slot(12, 9));
        long second = scheduleOk(receptionist.bearer(), patient, doctor.id(), slot(12, 10));

        // moving the first appointment onto the second one's slot is refused
        mvc.perform(put("/api/appointments/" + first + "/reschedule").header(AUTH, receptionist.bearer())
                        .contentType(JSON).content("{\"startTime\":\"%s\"}".formatted(slot(12, 10))))
                .andExpect(status().isConflict());

        // moving it to a free slot works
        mvc.perform(put("/api/appointments/" + first + "/reschedule").header(AUTH, receptionist.bearer())
                        .contentType(JSON).content("{\"startTime\":\"%s\"}".formatted(slot(12, 14))))
                .andExpect(status().isOk());

        // another doctor cannot complete it, the assigned doctor can, and only once
        mvc.perform(post("/api/appointments/" + first + "/complete").header(AUTH, otherDoctor.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/appointments/" + first + "/complete").header(AUTH, doctor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(post("/api/appointments/" + first + "/complete").header(AUTH, doctor.bearer()))
                .andExpect(status().isConflict());

        // cancelling frees the slot again
        mvc.perform(post("/api/appointments/" + second + "/cancel").header(AUTH, receptionist.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        schedule(receptionist.bearer(), patient, doctor.id(), slot(12, 10))
                .andExpect(status().isCreated());

        // doctors only see their own schedule
        mvc.perform(get("/api/appointments").header(AUTH, otherDoctor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/appointments").header(AUTH, doctor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    // ---------- clinical records ----------

    @Test
    void clinicalRecordsFollowTheAccessRules() throws Exception {
        String admin = adminBearer();
        Staff receptionist = createStaff(admin, "RECEPTIONIST");
        Staff nurse = createStaff(admin, "NURSE");
        Staff author = createStaff(admin, "DOCTOR");
        Staff colleague = createStaff(admin, "DOCTOR");   // also treats the patient, but did not write the record
        Staff outsider = createStaff(admin, "DOCTOR");    // has no appointment with the patient
        long patient = createPatient(receptionist.bearer(), "Patient " + uniqueToken());
        scheduleOk(receptionist.bearer(), patient, author.id(), slot(13, 9));
        scheduleOk(receptionist.bearer(), patient, colleague.id(), slot(13, 10));

        String createUrl = "/api/patients/" + patient + "/records";

        // who may NOT create a record
        for (String bearer : new String[]{outsider.bearer(), receptionist.bearer(), nurse.bearer(), admin}) {
            mvc.perform(post(createUrl).header(AUTH, bearer).contentType(JSON).content(RECORD_JSON))
                    .andExpect(status().isForbidden());
        }

        // the treating doctor creates one (version 1)
        MvcResult created = mvc.perform(post(createUrl).header(AUTH, author.bearer()).contentType(JSON).content(RECORD_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andReturn();
        long recordId = body(created).get("id").asLong();
        String recordUrl = "/api/records/" + recordId;

        // who may NOT read it: admin, receptionist, a doctor with no care relationship
        for (String bearer : new String[]{admin, receptionist.bearer(), outsider.bearer()}) {
            mvc.perform(get(recordUrl).header(AUTH, bearer)).andExpect(status().isForbidden());
            mvc.perform(get(createUrl).header(AUTH, bearer)).andExpect(status().isForbidden());
        }

        // who may read it: the author, a colleague who treats the patient, and nurses
        for (String bearer : new String[]{author.bearer(), colleague.bearer(), nurse.bearer()}) {
            mvc.perform(get(recordUrl).header(AUTH, bearer))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.diagnosis").value("Hypertension"));
        }
        mvc.perform(get(createUrl).header(AUTH, nurse.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // only the author edits, and an edit needs a reason
        String edit = "{\"diagnosis\":\"Hypertension stage 2\",\"treatment\":\"Medication\",\"changeReason\":\"%s\"}";
        mvc.perform(put(recordUrl).header(AUTH, colleague.bearer()).contentType(JSON).content(edit.formatted("Re-assessed")))
                .andExpect(status().isForbidden());
        mvc.perform(put(recordUrl).header(AUTH, nurse.bearer()).contentType(JSON).content(edit.formatted("Re-assessed")))
                .andExpect(status().isForbidden());
        mvc.perform(put(recordUrl).header(AUTH, author.bearer()).contentType(JSON)
                        .content("{\"diagnosis\":\"Hypertension stage 2\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(recordUrl).header(AUTH, author.bearer()).contentType(JSON).content(edit.formatted("Re-assessed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion").value(2))
                .andExpect(jsonPath("$.diagnosis").value("Hypertension stage 2"));

        // the original content is preserved in the history
        mvc.perform(get(recordUrl + "/history").header(AUTH, colleague.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versionNumber").value(1))
                .andExpect(jsonPath("$[0].diagnosis").value("Hypertension"))
                .andExpect(jsonPath("$[0].changeReason").value("Initial entry"))
                .andExpect(jsonPath("$[1].versionNumber").value(2))
                .andExpect(jsonPath("$[1].changeReason").value("Re-assessed"));
    }

    // ---------- audit trail ----------

    @Test
    void auditTrailRecordsAccessAndFailedLogins() throws Exception {
        String admin = adminBearer();
        Staff receptionist = createStaff(admin, "RECEPTIONIST");
        Staff nurse = createStaff(admin, "NURSE");
        Staff doctor = createStaff(admin, "DOCTOR");
        long patient = createPatient(receptionist.bearer(), "Patient " + uniqueToken());
        scheduleOk(receptionist.bearer(), patient, doctor.id(), slot(14, 9));
        long recordId = createRecord(doctor.bearer(), patient);

        mvc.perform(get("/api/records/" + recordId).header(AUTH, nurse.bearer())).andExpect(status().isOk());

        // the creation and the nurse's read are both on record
        mvc.perform(get("/api/admin/audit").header(AUTH, admin)
                        .param("entityType", "MedicalRecord").param("entityId", String.valueOf(recordId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action=='CREATE' && @.actorEmail=='%s')]".formatted(doctor.email())).isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action=='VIEW' && @.actorRole=='NURSE')]").isNotEmpty());

        // only admins can read the audit log
        mvc.perform(get("/api/admin/audit").header(AUTH, doctor.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").header(AUTH, nurse.bearer())).andExpect(status().isForbidden());

        // failed logins are logged against the email that was tried
        String attacker = "nobody-" + uniqueToken() + "@hospital.test";
        mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"guess\"}".formatted(attacker)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/audit").header(AUTH, admin)
                        .param("action", "LOGIN_FAILED").param("actorEmail", attacker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------- deactivation ----------

    @Test
    void deactivatedStaffAndPatientsAreBlocked() throws Exception {
        String admin = adminBearer();
        Staff receptionist = createStaff(admin, "RECEPTIONIST");
        Staff doctor = createStaff(admin, "DOCTOR");
        Staff spare = createStaff(admin, "NURSE");

        // deactivating an account kills its existing token and blocks new logins
        mvc.perform(patch("/api/admin/staff/" + spare.id() + "/active").header(AUTH, admin)
                        .contentType(JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/auth/me").header(AUTH, spare.bearer())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(spare.email(), STAFF_PASSWORD)))
                .andExpect(status().isUnauthorized());

        // an admin cannot lock themselves out
        long adminId = body(mvc.perform(get("/api/auth/me").header(AUTH, admin)).andReturn()).get("id").asLong();
        mvc.perform(patch("/api/admin/staff/" + adminId + "/active").header(AUTH, admin)
                        .contentType(JSON).content("{\"active\":false}"))
                .andExpect(status().isBadRequest());

        // patients: only admins deactivate; deactivated patients vanish from search and cannot be booked
        String token = uniqueToken();
        long patient = createPatient(receptionist.bearer(), "Patient " + token);
        mvc.perform(patch("/api/patients/" + patient + "/active").header(AUTH, receptionist.bearer())
                        .contentType(JSON).content("{\"active\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/patients/" + patient + "/active").header(AUTH, admin)
                        .contentType(JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/patients").header(AUTH, receptionist.bearer()).param("q", token))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/patients").header(AUTH, receptionist.bearer()).param("q", token).param("active", "false"))
                .andExpect(jsonPath("$.totalElements").value(1));
        schedule(receptionist.bearer(), patient, doctor.id(), slot(15, 9))
                .andExpect(status().isBadRequest());
    }
}
