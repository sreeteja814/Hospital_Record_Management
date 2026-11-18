import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, qs } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useFetch } from '../hooks.js'
import { isClinical, isFrontDesk } from '../roles.js'
import { ageFrom, fmtDate, fmtInstant, fmtTime } from '../format.js'
import { Alert, Empty, ErrorNote, Loading, StatusTag, Tag } from '../components/ui.jsx'
import PatientForm from '../components/PatientForm.jsx'
import AppointmentForm from '../components/AppointmentForm.jsx'
import RecordsPanel from '../components/RecordsPanel.jsx'

const GENDER = { FEMALE: 'Female', MALE: 'Male', OTHER: 'Other' }

function DetailRow({ label, children }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{children || <span className="muted">Not recorded</span>}</dd>
    </div>
  )
}

function DetailsTab({ patient, reload }) {
  const { user } = useAuth()
  const [editing, setEditing] = useState(false)
  const [error, setError] = useState(null)

  async function save(values) {
    await api('PUT', `/api/patients/${patient.id}`, values)
    setEditing(false)
    reload()
  }

  async function setActive(active) {
    const text = active
      ? `Reactivate ${patient.fullName}?`
      : `Deactivate ${patient.fullName}? They will no longer appear in search and cannot be booked. Their history is kept.`
    if (!window.confirm(text)) return
    setError(null)
    try {
      await api('PATCH', `/api/patients/${patient.id}/active`, { active })
      reload()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <div className="section-head">
        <h2>Patient details</h2>
        <div className="page-actions">
          {isFrontDesk(user.role) && (
            <button type="button" className="btn" onClick={() => setEditing(true)}>
              Edit details
            </button>
          )}
          {user.role === 'ADMIN' && (
            <button
              type="button"
              className={`btn${patient.active ? ' danger' : ''}`}
              onClick={() => setActive(!patient.active)}
            >
              {patient.active ? 'Deactivate patient' : 'Reactivate patient'}
            </button>
          )}
        </div>
      </div>
      {error && <Alert kind="error">{error}</Alert>}
      <dl className="details">
        <DetailRow label="Date of birth">
          {fmtDate(patient.dateOfBirth)} ({ageFrom(patient.dateOfBirth)} years)
        </DetailRow>
        <DetailRow label="Gender">{GENDER[patient.gender]}</DetailRow>
        <DetailRow label="Phone">{patient.phone}</DetailRow>
        <DetailRow label="Email">{patient.email}</DetailRow>
        <DetailRow label="Address">{patient.address}</DetailRow>
        <DetailRow label="Blood group">{patient.bloodGroup}</DetailRow>
        <DetailRow label="Emergency contact">
          {patient.emergencyContactName
            ? `${patient.emergencyContactName}${patient.emergencyContactPhone ? `, ${patient.emergencyContactPhone}` : ''}`
            : ''}
        </DetailRow>
        <DetailRow label="Registered">{fmtInstant(patient.createdAt)}</DetailRow>
      </dl>

      {editing && (
        <PatientForm
          title="Edit patient details"
          submitLabel="Save changes"
          initial={patient}
          onSave={save}
          onClose={() => setEditing(false)}
        />
      )}
    </>
  )
}

function AppointmentsTab({ patient }) {
  const { user } = useAuth()
  const [booking, setBooking] = useState(false)
  const { data, error, loading, reload } = useFetch(
    () => api('GET', `/api/appointments${qs({ patientId: patient.id, size: 50, sort: 'startTime,desc' })}`),
    [patient.id],
  )

  return (
    <>
      <div className="section-head">
        <h2>Appointments</h2>
        {isFrontDesk(user.role) && patient.active && (
          <button type="button" className="btn primary" onClick={() => setBooking(true)}>
            Book appointment
          </button>
        )}
      </div>
      <ErrorNote error={error} />
      <div className="table-wrap">
        {loading && !data ? (
          <Loading />
        ) : data && data.content.length === 0 ? (
          <Empty title="No appointments">
            {user.role === 'DOCTOR' ? 'You have no appointments with this patient.' : 'Nothing has been booked for this patient.'}
          </Empty>
        ) : (
          data && (
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>Doctor</th>
                  <th>Status</th>
                  <th>Reason</th>
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
                  </tr>
                ))}
              </tbody>
            </table>
          )
        )}
      </div>

      {booking && (
        <AppointmentForm
          mode="create"
          fixedPatient={patient}
          onClose={() => setBooking(false)}
          onDone={() => {
            setBooking(false)
            reload()
          }}
        />
      )}
    </>
  )
}

export default function PatientDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const clinical = isClinical(user.role)
  const [tab, setTab] = useState(clinical ? 'records' : 'details')
  const { data: patient, error, loading, reload } = useFetch(() => api('GET', `/api/patients/${id}`), [id])

  const tabs = [
    ...(clinical ? [{ key: 'records', label: 'Clinical records' }] : []),
    { key: 'appointments', label: 'Appointments' },
    { key: 'details', label: 'Details' },
  ]

  return (
    <>
      <Link to="/patients" className="back">
        Back to patients
      </Link>

      <ErrorNote error={error} />
      {loading && !patient && <Loading />}

      {patient && (
        <>
          <header className={`chart-head${patient.active ? '' : ' inactive'}`}>
            <div className="title-row">
              <h1>{patient.fullName}</h1>
              <span className="mrn">{patient.mrn}</span>
              {!patient.active && <Tag>Deactivated</Tag>}
            </div>
            <dl className="chart-facts">
              <div>
                <dt>Date of birth</dt>
                <dd>
                  {fmtDate(patient.dateOfBirth)} ({ageFrom(patient.dateOfBirth)})
                </dd>
              </div>
              <div>
                <dt>Gender</dt>
                <dd>{GENDER[patient.gender]}</dd>
              </div>
              <div>
                <dt>Blood group</dt>
                <dd>{patient.bloodGroup || 'Not recorded'}</dd>
              </div>
              <div>
                <dt>Phone</dt>
                <dd>{patient.phone}</dd>
              </div>
            </dl>
          </header>

          {!patient.active && (
            <Alert kind="warn">This patient is deactivated. They cannot be booked, but their history is kept.</Alert>
          )}

          <div className="tabs" role="tablist">
            {tabs.map((t) => (
              <button
                key={t.key}
                type="button"
                role="tab"
                aria-selected={tab === t.key}
                onClick={() => setTab(t.key)}
              >
                {t.label}
              </button>
            ))}
          </div>

          {tab === 'records' && clinical && <RecordsPanel patient={patient} />}
          {tab === 'appointments' && <AppointmentsTab patient={patient} />}
          {tab === 'details' && <DetailsTab patient={patient} reload={reload} />}
        </>
      )}
    </>
  )
}
