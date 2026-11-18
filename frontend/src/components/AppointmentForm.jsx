import { useState } from 'react'
import { api, qs } from '../api.js'
import { useDebounced, useFetch } from '../hooks.js'
import { fmtDate, fmtTime, nowLocalInput } from '../format.js'
import { Alert, Field, Modal } from './ui.jsx'

const DURATIONS = [15, 30, 45, 60, 90]

function PatientPicker({ value, onChange, error }) {
  const [text, setText] = useState('')
  const search = useDebounced(text.trim(), 300)
  const { data } = useFetch(
    () => (search.length >= 2 ? api('GET', `/api/patients${qs({ q: search, size: 6 })}`) : Promise.resolve(null)),
    [search],
  )

  if (value) {
    return (
      <div className="field">
        <span className="field-label">Patient</span>
        <div className="picked">
          <span>
            <strong>{value.fullName}</strong> <span className="mrn">{value.mrn}</span>
          </span>
          <button type="button" className="btn small" onClick={() => onChange(null)}>
            Change
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="field">
      <label htmlFor="patient-search" className="field-label">
        Patient
      </label>
      <input
        id="patient-search"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Search by name, MRN or phone"
        autoComplete="off"
      />
      {error && <span className="field-error">{error}</span>}
      {data && data.content.length > 0 && (
        <div className="picker-results">
          {data.content.map((p) => (
            <button key={p.id} type="button" onClick={() => onChange(p)}>
              <strong>{p.fullName}</strong>
              <span className="mrn">{p.mrn}</span>
            </button>
          ))}
        </div>
      )}
      {data && data.content.length === 0 && <span className="hint">No active patient matches that search.</span>}
    </div>
  )
}

// mode "create": book a new appointment (optionally for a fixed patient).
// mode "reschedule": move an existing appointment to a new time.
export default function AppointmentForm({ mode, appointment, fixedPatient, onClose, onDone }) {
  const rescheduling = mode === 'reschedule'
  const doctors = useFetch(() => (rescheduling ? Promise.resolve([]) : api('GET', '/api/doctors')), [rescheduling])

  const [patient, setPatient] = useState(fixedPatient || null)
  const [doctorId, setDoctorId] = useState('')
  const [startTime, setStartTime] = useState('')
  const [duration, setDuration] = useState(30)
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setFieldErrors({})

    if (!rescheduling && !patient) {
      setFieldErrors({ patientId: 'Choose a patient.' })
      return
    }
    if (!rescheduling && !doctorId) {
      setFieldErrors({ doctorId: 'Choose a doctor.' })
      return
    }
    if (!startTime) {
      setFieldErrors({ startTime: 'Choose a date and time.' })
      return
    }

    setBusy(true)
    try {
      if (rescheduling) {
        await api('PUT', `/api/appointments/${appointment.id}/reschedule`, {
          startTime,
          durationMinutes: Number(duration),
        })
      } else {
        await api('POST', '/api/appointments', {
          patientId: patient.id,
          doctorId: Number(doctorId),
          startTime,
          durationMinutes: Number(duration),
          reason: reason.trim() || null,
        })
      }
      onDone()
    } catch (err) {
      setFieldErrors(err.fieldErrors || {})
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={rescheduling ? 'Reschedule appointment' : 'Book appointment'} onClose={onClose}>
      {error && <Alert kind="error">{error}</Alert>}
      <form onSubmit={submit} noValidate>
        {rescheduling ? (
          <p className="muted" style={{ marginBottom: '1rem' }}>
            {appointment.patientName} with {appointment.doctorName}, currently {fmtDate(appointment.startTime)} at{' '}
            {fmtTime(appointment.startTime)}.
          </p>
        ) : (
          <>
            <PatientPicker value={patient} onChange={setPatient} error={fieldErrors.patientId} />
            <Field label="Doctor" error={fieldErrors.doctorId}>
              <select value={doctorId} onChange={(e) => setDoctorId(e.target.value)}>
                <option value="">Select a doctor</option>
                {(doctors.data || []).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.fullName}, {d.specialization}
                  </option>
                ))}
              </select>
            </Field>
          </>
        )}

        <div className="form-grid">
          <Field label="Date and time" error={fieldErrors.startTime}>
            <input
              type="datetime-local"
              value={startTime}
              min={nowLocalInput()}
              step={300}
              onChange={(e) => setStartTime(e.target.value)}
            />
          </Field>
          <Field label="Length" error={fieldErrors.durationMinutes}>
            <select value={duration} onChange={(e) => setDuration(e.target.value)}>
              {DURATIONS.map((m) => (
                <option key={m} value={m}>
                  {m} minutes
                </option>
              ))}
            </select>
          </Field>
        </div>

        {!rescheduling && (
          <Field label="Reason (optional)" error={fieldErrors.reason}>
            <input value={reason} onChange={(e) => setReason(e.target.value)} maxLength={255} />
          </Field>
        )}

        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn primary" disabled={busy}>
            {busy ? 'Saving' : rescheduling ? 'Reschedule' : 'Book appointment'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
