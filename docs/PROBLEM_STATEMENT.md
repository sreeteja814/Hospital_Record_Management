# Problem Statement

## Title

**Hospital Patient Record Management System: a secure backend with role-based access control and a full audit trail**

## Background

Many small and mid-sized hospitals and clinics still keep patient information in paper files, spreadsheets or loosely connected tools. This causes problems that affect both patient care and compliance:

- The same patient is registered more than once, with inconsistent details.
- A doctor gets double-booked because two people schedule at the same time.
- Anyone at the front desk can open clinical notes that they have no reason to see.
- When a diagnosis is corrected, the earlier version is overwritten, so nobody can tell what was recorded before or who changed it.
- There is no trace of who viewed or modified a patient's file, so a privacy breach cannot be detected or investigated.

Medical data is among the most sensitive personal data there is, and it is regulated (India's Digital Personal Data Protection Act, 2023; HIPAA in the United States). These gaps are therefore a legal and ethical risk as well as an operational one.

## Proposed solution

A backend service (REST API) that digitises the core hospital workflow with security built in rather than added later:

- **Staff accounts with roles.** Administrators, doctors, nurses and receptionists each get only the access their job needs. There is no public sign-up; an administrator creates accounts.
- **Patient registry.** One record per patient with a unique medical record number (MRN). Patients are deactivated, never deleted, so their history stays intact.
- **Appointment scheduling with conflict detection.** A doctor cannot be booked into overlapping slots, even when two receptionists book at the same moment.
- **Clinical records with version history.** Doctors record diagnosis, treatment and prescription. Every edit requires a reason and keeps the previous version, and two simultaneous edits cannot silently overwrite each other.
- **Patient logins.** Patients create their own login (verified with their MRN and date of birth) and can book, view and cancel only their own appointments. They never see clinical records.
- **Least-privilege access to clinical data.** Only doctors who treat a patient can read that patient's records, and only the authoring doctor can edit them. Nurses can read. Receptionists and administrators cannot see clinical content.
- **Audit trail.** Every login (successful and failed), view, creation and change is logged with who, what and when, and only administrators can read the log. It never contains clinical content.

## Objectives

1. Provide JWT-secured REST APIs for staff authentication, patients, appointments and clinical records.
2. Enforce role-based access and a doctor-patient care relationship on every clinical operation.
3. Prevent double-booking of doctors under concurrent requests, including bookings made by patients themselves.
4. Preserve complete edit history for clinical records and an append-only audit log for all sensitive actions.
5. Validate all input and return consistent, meaningful error responses.
6. Cover the key rules with automated end-to-end tests.

## Scope

**Included:** staff and role management, patient registry, appointment scheduling, clinical records with versioning, audit log, search and pagination, patient self-service booking, a role-aware React web interface, PostgreSQL support and an in-memory database for development.

**Not included in this version:** billing and insurance, pharmacy and inventory, ward and bed management, lab integrations, patients viewing their own results, and notifications. These are listed as future work.

## Users

| Role | What they do in the system |
|------|----------------------------|
| Administrator | Manage staff accounts, deactivate patients, review the audit log |
| Receptionist | Register patients, book, reschedule and cancel appointments |
| Doctor | See their own schedule, write and edit clinical records for patients they treat |
| Nurse | Read clinical records (read-only) |
| Patient | Create their own login, book, view and cancel their own appointments |

## Technology

Backend: Java 21, Spring Boot 3.5, Spring Security with JWT, Spring Data JPA (Hibernate), H2 (development) and PostgreSQL, Maven, JUnit 5 with MockMvc. Frontend: React 19, React Router and Vite.

## Expected outcome

A working, tested API that demonstrates secure backend design: authentication, authorization beyond simple roles, concurrency control, immutable history and auditability, built on realistic hospital workflows.

> The system is built and demonstrated with fictional data only. Real patient data requires legal, security and compliance review before use.
