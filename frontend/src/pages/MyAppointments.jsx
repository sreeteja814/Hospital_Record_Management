import { useState } from 'react'
import { api, qs } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useFetch } from '../hooks.js'
import { fmtDate, fmtTime, nowLocalInput } from '../format.js'
import { Alert, Empty, ErrorNote, Field, Loading, Modal, PageHead, Pager, StatusTag } from '../components/ui.jsx'

// A patient's own area: their appointments, and booking a new one. The server decides who the patient is
// from the login, so nothing here can be pointed at someone else's appointments.
function BookingForm({ onClose, onDone }) {
  const doctors = useFetch(() => api('GET', '/api/doctors'), [])
  const [doctorId, setDoctorId] = useState('')
  const [startTime, setStartTime] = useState('')
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    if (!doctorId) return setFieldErrors({ doctorId: 'Choose a doctor.' })
    if (!startTime) return setFieldErrors({ startTime: 'Choose a date and time.' })

    setBusy(true)
    try {
      await api('POST', '/api/me/appointments', {
        doctorId: Number(doctorId),
        startTime,
        reason: reason.trim() || null,
      })
      onDone()
    } catch (err) {
      setFieldErrors(err.fieldErrors || {})
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="Book an appointment" onClose={onClose}>
      {error && <Alert kind="error">{error}</Alert>}
      <form onSubmit={submit} noValidate>
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
        <Field label="Date and time" error={fieldErrors.startTime} hint="Appointments are 30 minutes.">
          <input
            type="datetime-local"
            value={startTime}
            min={nowLocalInput()}
            step={300}
            onChange={(e) => setStartTime(e.target.value)}
          />
        </Field>
        <Field label="Reason (optional)" error={fieldErrors.reason}>
          <input value={reason} onChange={(e) => setReason(e.target.value)} maxLength={255} />
        </Field>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn primary" disabled={busy}>
            {busy ? 'Booking' : 'Book appointment'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default function MyAppointments() {
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const [booking, setBooking] = useState(false)
  const [actionError, setActionError] = useState(null)
  const [notice, setNotice] = useState(null)

  const { data, error, loading, reload } = useFetch(
    () => api('GET', `/api/me/appointments${qs({ page, size: 15, sort: 'startTime,desc' })}`),
    [page],
  )

  async function cancel(a) {
    if (!window.confirm(`Cancel your appointment with ${a.doctorName} on ${fmtDate(a.startTime)}?`)) return
    setActionError(null)
    setNotice(null)
    try {
      await api('POST', `/api/me/appointments/${a.id}/cancel`)
      setNotice('Your appointment was cancelled.')
      reload()
    } catch (err) {
      setActionError(err.message)
    }
  }

  return (
    <>
      <PageHead title="My appointments" subtitle={`Signed in as ${user.fullName}. Latest first.`}>
        <button type="button" className="btn primary" onClick={() => setBooking(true)}>
          Book appointment
        </button>
      </PageHead>

      {notice && <Alert kind="info">{notice}</Alert>}
      {actionError && <Alert kind="error">{actionError}</Alert>}
      <ErrorNote error={error} />

      <div className="table-wrap">
        {loading && !data ? (
          <Loading />
        ) : data && data.content.length === 0 ? (
          <Empty title="No appointments yet">Use "Book appointment" to see a doctor.</Empty>
        ) : (
          data && (
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>Doctor</th>
                  <th>Status</th>
                  <th>Reason</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.content.map((a) => (
                  <tr key={a.id}>
                    <td>
                      <strong>{fmtDate(a.startTime)}</strong>
                      <span className="sub">
                        {fmtTime(a.startTime)} to {fmtTime(a.endTime)}
                      </span>
                    </td>
                    <td>{a.doctorName}</td>
                    <td>
                      <StatusTag status={a.status} />
                    </td>
                    <td>{a.reason}</td>
                    <td className="actions">
                      {a.status === 'SCHEDULED' && (
                        <button type="button" className="btn small danger" onClick={() => cancel(a)}>
                          Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        )}
      </div>

      {data && <Pager page={data.page} totalPages={data.totalPages} total={data.totalElements} onPage={setPage} />}

      {booking && (
        <BookingForm
          onClose={() => setBooking(false)}
          onDone={() => {
            setBooking(false)
            setNotice('Your appointment is booked.')
            reload()
          }}
        />
      )}
    </>
  )
}
