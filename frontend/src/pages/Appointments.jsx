import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, qs } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useDebounced, useFetch } from '../hooks.js'
import { isFrontDesk } from '../roles.js'
import { ageFrom, fmtDate, fmtTime, todayISO } from '../format.js'
import { Alert, Empty, ErrorNote, Loading, PageHead, Pager, StatusTag } from '../components/ui.jsx'
import AppointmentForm from '../components/AppointmentForm.jsx'

export default function Appointments() {
  const { user } = useAuth()
  const isDoctor = user.role === 'DOCTOR'
  const [date, setDate] = useState('')
  const [status, setStatus] = useState('')
  const [doctorId, setDoctorId] = useState('')
  const [page, setPage] = useState(0)
  const [booking, setBooking] = useState(false)
  const [rescheduling, setRescheduling] = useState(null)
  const [actionError, setActionError] = useState(null)

  useEffect(() => {
    setPage(0)
  }, [date, status, doctorId])

  // Patient finder for clinical staff: search by name, MRN or phone, then open the patient to see their history.
  const canSearchPatients = isDoctor || user.role === 'NURSE'
  const [patientQ, setPatientQ] = useState('')
  const patientSearch = useDebounced(patientQ.trim(), 300)
  const patientResults = useFetch(
    () =>
      canSearchPatients && patientSearch.length >= 2
        ? api('GET', `/api/patients${qs({ q: patientSearch, active: true, page: 0, size: 6 })}`)
        : Promise.resolve(null),
    [canSearchPatients, patientSearch],
  )

  const doctors = useFetch(() => (isDoctor ? Promise.resolve([]) : api('GET', '/api/doctors')), [isDoctor])

  const { data, error, loading, reload } = useFetch(
    () =>
      api(
        'GET',
        `/api/appointments${qs({ date, status, doctorId, page, size: 15, sort: 'startTime,desc' })}`,
      ),
    [date, status, doctorId, page],
  )

  async function act(appointment, verb, confirmText) {
    if (confirmText && !window.confirm(confirmText)) return
    setActionError(null)
    try {
      await api('POST', `/api/appointments/${appointment.id}/${verb}`)
      reload()
    } catch (err) {
      setActionError(err.message)
    }
  }

  return (
    <>
      <PageHead
        title={isDoctor ? 'My schedule' : 'Appointments'}
        subtitle={isDoctor ? 'Your appointments, latest first.' : 'All appointments, latest first.'}
      >
        {isFrontDesk(user.role) && (
          <button type="button" className="btn primary" onClick={() => setBooking(true)}>
            Book appointment
          </button>
        )}
      </PageHead>

      {canSearchPatients && (
        <div style={{ marginBottom: '1.5rem' }}>
          <div className="filters">
            <label className="field grow">
              <span className="field-label">Find a patient</span>
              <input
                type="search"
                value={patientQ}
                onChange={(e) => setPatientQ(e.target.value)}
                placeholder="Search by name, MRN or phone to open their history"
              />
            </label>
          </div>
          {patientSearch.length >= 2 && (
            <>
              <ErrorNote error={patientResults.error} />
              <div className="table-wrap">
                {patientResults.loading && !patientResults.data ? (
                  <Loading />
                ) : patientResults.data && patientResults.data.content.length === 0 ? (
                  <Empty title="No patients found">Check the spelling, or try the MRN or phone number.</Empty>
                ) : (
                  patientResults.data && (
                    <table>
                      <thead>
                        <tr>
                          <th>Patient</th>
                          <th>MRN</th>
                          <th>Date of birth</th>
                          <th />
                        </tr>
                      </thead>
                      <tbody>
                        {patientResults.data.content.map((p) => (
                          <tr key={p.id}>
                            <td>
                              <Link to={`/patients/${p.id}`}>
                                <strong>{p.fullName}</strong>
                              </Link>
                            </td>
                            <td>
                              <span className="mrn">{p.mrn}</span>
                            </td>
                            <td>
                              {fmtDate(p.dateOfBirth)}
                              <span className="sub">{ageFrom(p.dateOfBirth)} years</span>
                            </td>
                            <td className="actions">
                              <Link className="btn small" to={`/patients/${p.id}`}>
                                View history
                              </Link>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )
                )}
              </div>
            </>
          )}
        </div>
      )}

      <div className="filters">
        <label className="field">
          <span className="field-label">Date</span>
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <button type="button" className="btn" onClick={() => setDate(todayISO())}>
          Today
        </button>
        <label className="field">
          <span className="field-label">Status</span>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Any status</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </label>
        {!isDoctor && (
          <label className="field">
            <span className="field-label">Doctor</span>
            <select value={doctorId} onChange={(e) => setDoctorId(e.target.value)}>
              <option value="">All doctors</option>
              {(doctors.data || []).map((d) => (
                <option key={d.id} value={d.id}>
                  {d.fullName}
                </option>
              ))}
            </select>
          </label>
        )}
        {(date || status || doctorId) && (
          <button
            type="button"
            className="btn"
            onClick={() => {
              setDate('')
              setStatus('')
              setDoctorId('')
            }}
          >
            Clear filters
          </button>
        )}
      </div>

      {actionError && <Alert kind="error">{actionError}</Alert>}
      <ErrorNote error={error} />

      <div className="table-wrap">
        {loading && !data ? (
          <Loading />
        ) : data && data.content.length === 0 ? (
          <Empty title="No appointments found">
            {date || status || doctorId ? 'Try clearing the filters.' : 'Booked appointments will appear here.'}
          </Empty>
        ) : (
          data && (
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>Patient</th>
                  {!isDoctor && <th>Doctor</th>}
                  <th>Status</th>
                  <th>Reason</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.content.map((a) => {
                  const open = a.status === 'SCHEDULED'
                  return (
                    <tr key={a.id}>
                      <td>
                        <strong>{fmtDate(a.startTime)}</strong>
                        <span className="sub">
                          {fmtTime(a.startTime)} to {fmtTime(a.endTime)}
                        </span>
                      </td>
                      <td>
                        <Link to={`/patients/${a.patientId}`}>{a.patientName}</Link>
                        <span className="sub">{a.patientMrn}</span>
                      </td>
                      {!isDoctor && <td>{a.doctorName}</td>}
                      <td>
                        <StatusTag status={a.status} />
                      </td>
                      <td>{a.reason}</td>
                      <td className="actions">
                        {open && isFrontDesk(user.role) && (
                          <button type="button" className="btn small" onClick={() => setRescheduling(a)}>
                            Reschedule
                          </button>
                        )}
                        {open && isDoctor && (
                          <button type="button" className="btn small primary" onClick={() => act(a, 'complete')}>
                            Mark completed
                          </button>
                        )}
                        {open && user.role !== 'NURSE' && (
                          <button
                            type="button"
                            className="btn small danger"
                            onClick={() =>
                              act(a, 'cancel', `Cancel the appointment for ${a.patientName} on ${fmtDate(a.startTime)}?`)
                            }
                          >
                            Cancel
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )
        )}
      </div>

      {data && <Pager page={data.page} totalPages={data.totalPages} total={data.totalElements} onPage={setPage} />}

      {booking && (
        <AppointmentForm
          mode="create"
          onClose={() => setBooking(false)}
          onDone={() => {
            setBooking(false)
            reload()
          }}
        />
      )}
      {rescheduling && (
        <AppointmentForm
          mode="reschedule"
          appointment={rescheduling}
          onClose={() => setRescheduling(null)}
          onDone={() => {
            setRescheduling(null)
            reload()
          }}
        />
      )}
    </>
  )
}
