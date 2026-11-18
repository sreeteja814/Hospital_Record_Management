import { useEffect, useState } from 'react'
import { api, qs } from '../api.js'
import { useDebounced, useFetch } from '../hooks.js'
import { fmtInstant } from '../format.js'
import { Empty, ErrorNote, Loading, PageHead, Pager, Tag } from '../components/ui.jsx'

const ACTIONS = {
  LOGIN: { label: 'Signed in', tone: '' },
  LOGIN_FAILED: { label: 'Failed sign-in', tone: 'red' },
  CREATE: { label: 'Created', tone: 'green' },
  VIEW: { label: 'Viewed', tone: '' },
  UPDATE: { label: 'Updated', tone: 'amber' },
  DEACTIVATE: { label: 'Deactivated', tone: 'red' },
  ACTIVATE: { label: 'Reactivated', tone: 'green' },
  RESCHEDULE: { label: 'Rescheduled', tone: 'amber' },
  CANCEL: { label: 'Cancelled', tone: 'red' },
  COMPLETE: { label: 'Completed', tone: 'green' },
}

const ENTITY_TYPES = {
  Patient: 'Patient',
  MedicalRecord: 'Medical record',
  Appointment: 'Appointment',
  StaffUser: 'Staff account',
}

export default function Audit() {
  const [actorEmail, setActorEmail] = useState('')
  const [action, setAction] = useState('')
  const [entityType, setEntityType] = useState('')
  const [entityId, setEntityId] = useState('')
  const [page, setPage] = useState(0)

  const email = useDebounced(actorEmail.trim(), 400)
  const id = useDebounced(entityId.trim(), 400)

  useEffect(() => {
    setPage(0)
  }, [email, action, entityType, id])

  const { data, error, loading } = useFetch(
    () => api('GET', `/api/admin/audit${qs({ actorEmail: email, action, entityType, entityId: id, page, size: 20 })}`),
    [email, action, entityType, id, page],
  )

  return (
    <>
      <PageHead
        title="Audit log"
        subtitle="Who did what, and when. Clinical content is never stored here, and entries cannot be changed."
      />

      <div className="filters">
        <label className="field grow">
          <span className="field-label">Staff email</span>
          <input value={actorEmail} onChange={(e) => setActorEmail(e.target.value)} placeholder="dr.rao@hospital.test" />
        </label>
        <label className="field">
          <span className="field-label">Action</span>
          <select value={action} onChange={(e) => setAction(e.target.value)}>
            <option value="">Any action</option>
            {Object.entries(ACTIONS).map(([key, a]) => (
              <option key={key} value={key}>
                {a.label}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span className="field-label">Item type</span>
          <select value={entityType} onChange={(e) => setEntityType(e.target.value)}>
            <option value="">Any item</option>
            {Object.entries(ENTITY_TYPES).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label className="field" style={{ minWidth: '7rem', maxWidth: '8rem' }}>
          <span className="field-label">Item number</span>
          <input
            value={entityId}
            onChange={(e) => setEntityId(e.target.value.replace(/\D/g, ''))}
            inputMode="numeric"
            placeholder="1"
          />
        </label>
      </div>

      <ErrorNote error={error} />

      <div className="table-wrap">
        {loading && !data ? (
          <Loading />
        ) : data && data.content.length === 0 ? (
          <Empty title="No matching entries">Try clearing a filter.</Empty>
        ) : (
          data && (
            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Staff member</th>
                  <th>Action</th>
                  <th>Item</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((e) => {
                  const a = ACTIONS[e.action] || { label: e.action, tone: '' }
                  return (
                    <tr key={e.id}>
                      <td>{fmtInstant(e.occurredAt)}</td>
                      <td>
                        {e.actorEmail}
                        <span className="sub">{e.actorRole.toLowerCase()}</span>
                      </td>
                      <td>
                        <Tag tone={a.tone}>{a.label}</Tag>
                      </td>
                      <td>
                        {ENTITY_TYPES[e.entityType] || e.entityType}
                        {e.entityId != null && ` ${e.entityId}`}
                      </td>
                      <td>{e.details}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )
        )}
      </div>

      {data && <Pager page={data.page} totalPages={data.totalPages} total={data.totalElements} onPage={setPage} />}
    </>
  )
}
