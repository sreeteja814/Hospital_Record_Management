# Hospital Records: web interface

The browser front end for the Hospital Patient Record Management System. It is a React app that talks to the
Spring Boot API in the parent folder. Every screen adapts to the signed-in person's role.

**Stack:** React 19, React Router 7, Vite 8. No UI framework: plain CSS in `src/styles.css`.

> Fictional data only. Do not enter real patient information.

## Requirements

- **Node.js 20.19 or newer** (22 LTS recommended). Check with `node -v`. Download from <https://nodejs.org> if needed.
- The backend running on `http://localhost:8080` (see the main README: `mvn spring-boot:run`).

## Run it

Two things must run at the same time, each in its own terminal.

**Terminal 1: the backend** (from the project root, the folder containing `pom.xml`)

```bash
mvn spring-boot:run
```

**Terminal 2: the web interface**

```bash
cd frontend
npm install        # first time only, takes about a minute
npm run dev
```

Open **<http://localhost:5173>**. The sign-in page has buttons that fill in the demo accounts:

| Role | What you will see |
|------|-------------------|
| Receptionist | Patients (register, edit), appointments (book, reschedule, cancel). No clinical records. |
| Doctor | "My schedule" (own appointments, mark completed), clinical records for patients they treat: add, edit own, version history. |
| Nurse | Patients and appointments, and clinical records read-only, including version history. |
| Administrator | Patients, appointments, staff accounts (add, deactivate), and the audit log. No clinical records. |
| Patient | "My appointments" only: book, view and cancel their own. Signs up with the MRN printed on their card and their date of birth. No clinical records. |

Patients sign up from the sign-in page (**Create your account**). The demo patient Asha Verma already has a login (the Patient demo button). To try sign-up yourself, sign in as the receptionist, register a new patient, note the MRN shown, sign out, then use **Create your account** with that MRN, the date of birth you entered and a new email.

Try this to see the access rules: sign in as the doctor, open Asha Verma, add a record, then edit it with a reason
and open "Version history". Then sign in as the receptionist and open the same patient. There is no clinical
tab. Finally sign in as the administrator and open the audit log to see every one of those actions.

## How it connects to the backend

The page is served by Vite at port 5173, and Vite forwards every request that starts with `/api` to the Spring Boot
backend at `http://127.0.0.1:8080` (see `vite.config.js`). Because the browser only ever talks to one address,
**no CORS configuration is needed on the backend**. To point at a different backend:

```bash
# macOS / Linux
API_URL=http://127.0.0.1:9090 npm run dev
# Windows PowerShell
$env:API_URL="http://127.0.0.1:9090"; npm run dev
```

## Things worth knowing

- **The interface hides what a role cannot use, but the backend decides.** Hiding a button is for convenience; every
  rule (who can read a record, who can edit it, double-booking) is enforced by the API, so the screens could not be
  tricked into breaking them.
- **The sign-in token lives in `sessionStorage`**, so it disappears when the browser tab is closed. If the token
  expires or the account is deactivated, the next request gets a `401` and the app signs you out automatically.
- **Editing a clinical record sends the whole record**, so a field you did not touch is never cleared by accident.
  A reason for the change is required and is stored in the version history.
- **Server errors are shown in plain words**, including field-by-field validation messages and the double-booking
  conflict message.

## Project structure

```
frontend/
  index.html
  vite.config.js            dev server + /api proxy
  src/
    main.jsx                entry point
    App.jsx                 routes and role guards
    api.js                  fetch wrapper: adds the token, turns API errors into readable messages
    auth.jsx                sign-in state (who is logged in)
    roles.js, hooks.js, format.js   small helpers
    components/             Layout, shared UI pieces, patient / appointment / records forms
    pages/                  Login, Patients, PatientDetail, Appointments, Staff, Audit
    styles.css
```

## Production build

```bash
npm run build      # creates frontend/dist (static files)
```

The files in `dist/` can be served by any web server. Serve them from the same address as the API (for example
with a reverse proxy that sends `/api` to Spring Boot), or add CORS configuration to the backend.

## Troubleshooting

- **"Cannot reach the server. Check that the backend is running on port 8080."** Start the backend first (Terminal 1).
- **`npm run dev` fails with a message about the Node version.** Install Node 20.19 or newer.
- **Port 5173 is already in use.** Vite picks the next free port and prints the address it chose. Use that one.
- **Everything shows "Sign in" again after a while.** Tokens last 60 minutes. Sign in again.
