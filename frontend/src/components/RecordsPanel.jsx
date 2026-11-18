import { useState } from 'react'
import { api, qs } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useFetch } from '../hooks.js'
import { clean, fmtInstant } from '../format.js'
import { Alert, Empty, ErrorNote, Field, Loading, Modal, Pager } from './ui.jsx'

function Row({ label, value }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{value || <span className="muted">Not recorded</span>}</dd>
    </>
  )
}

// Create a record (record is undefined) or edit one. Editing sends the full record, so nothing is cleared by accident.
function RecordForm({ patientId, record, onClose, onDone }) {
  const editing = Boolean(record)
  const [v, setV] = useState({
    diagnosis: record?.diagnosis ?? '',
    treatment: record?.treatment ?? '',
    prescription: record?.prescription ?? '',
    notes: record?.notes ?? '',
    changeReason: '',
  })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  const set = (key) => (e) => setV({ ...v, [key]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    if (editing && !v.changeReason.trim()) {
      setFieldErrors({ changeReason: 'Say why you are changing this record.' })
      return
    }
    setBusy(true)
    try {
      const body = clean(v)
      if (editing) await api('PUT', `/api/records/${record.id}`, body)
      else await api('POST', `/api/patients/${patientId}/records`, body)
      onDone()
    } catch (err) {
      setFieldErrors(err.fieldErrors || {})
      if (err.status === 403) {
        setError(
          editing
            ? 'You can only edit records of patients you have an appointment with.'
            : 'You can only add a record for a patient you have an appointment with.',
        )
      } else {
        setError(err.message)
      }
      setBusy(false)
    }
  }

  return (
    <Modal title={editing ? 'Edit clinical record' : 'New clinical record'} onClose={onClose} wide>
      {error && <Alert kind="error">{error}</Alert>}
      <form onSubmit={submit} noValidate>
        <Field label="Diagnosis" error={fieldErrors.diagnosis}>
          <input value={v.diagnosis} onChange={set('diagnosis')} maxLength={500} autoFocus />
        </Field>
        <Field label="Treatment" error={fieldErrors.treatment}>
          <textarea value={v.treatment} onChange={set('treatment')} maxLength={2000} />
        </Field>
        <Field label="Prescription" error={fieldErrors.prescription}>
          <textarea value={v.prescription} onChange={set('prescription')} maxLength={2000} />
        </Field>
        <Field label="Notes" error={fieldErrors.notes}>
          <textarea value={v.notes} onChange={set('notes')} maxLength={4000} />
        </Field>
        {editing && (
          <Field
            label="Reason for the change"
            error={fieldErrors.changeReason}
            hint="Saved in the version history. The earlier version stays available."
          >
            <input value={v.changeReason} onChange={set('changeReason')} maxLength={255} />
          </Field>
        )}
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn primary" disabled={busy}>
            {busy ? 'Saving' : editing ? 'Save changes' : 'Save record'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

function HistoryDialog({ record, onClose }) {
  const { data, error, loading } = useFetch(() => api('GET', `/api/records/${record.id}/history`), [record.id])
  const versions = data ? [...data].reverse() : []

  return (
    <Modal title={`Version history: ${record.diagnosis}`} onClose={onClose} wide>
      <ErrorNote error={error} />
      {loading && <Loading />}
      {versions.map((v) => (
        <section className="version" key={v.versionNumber}>
          <h3>Version {v.versionNumber}</h3>
          <div className="record-meta">
            <span>{v.editedBy}</span>
            <span>{fmtInstant(v.editedAt)}</span>
            <span>Reason: {v.changeReason}</span>
          </div>
          <dl>
            <Row label="Diagnosis" value={v.diagnosis} />
            <Row label="Treatment" value={v.treatment} />
            <Row label="Prescription" value={v.prescription} />
            <Row label="Notes" value={v.notes} />
          </dl>
        </section>
      ))}
    </Modal>
  )
}

export default function RecordsPanel({ patient }) {
  const { user } = useAuth()
  const canWrite = user.role === 'DOCTOR'
  const [page, setPage] = useState(0)
  const [form, setForm] = useState(null) // 'new' or the record being edited
  const [history, setHistory] = useState(null)

  const { data, error, loading, reload } = useFetch(
    () => api('GET', `/api/patients/${patient.id}/records${qs({ page, size: 10 })}`),
    [patient.id, page],
  )

  if (error && error.status === 403) {
    return (
      <Alert kind="warn">
        You do not have access to this patient&apos;s clinical records.{' '}
        {canWrite && 'Doctors can open records for patients they have an appointment with. Ask the front desk to book one.'}
      </Alert>
    )
  }

  return (
    <>
      <div className="section-head">
        <h2>Clinical records</h2>
        {canWrite && (
          <button type="button" className="btn primary" onClick={() => setForm('new')}>
            New record
          </button>
        )}
      </div>

      <ErrorNote error={error} />
      {loading && !data && <Loading />}

      {data && data.content.length === 0 && (
        <div className="table-wrap">
          <Empty title="No clinical records yet">
            {canWrite ? 'Add the first record after seeing the patient.' : 'Records written by doctors will appear here.'}
          </Empty>
        </div>
      )}

      {data &&
        data.content.map((r) => (
          <article className="record" key={r.id}>
            <div className="record-head">
              <div>
                <h3>{r.diagnosis}</h3>
                <div className="record-meta">
                  <span>Version {r.currentVersion}</span>
                  <span>{r.doctorName}</span>
                  <span>Updated {fmtInstant(r.updatedAt)}</span>
                </div>
              </div>
              <div className="page-actions">
                <button type="button" className="btn small" onClick={() => setHistory(r)}>
                  Version history
                </button>
                {canWrite && (
                  <button type="button" className="btn small" onClick={() => setForm(r)}>
                    Edit
                  </button>
                )}
              </div>
            </div>
            <dl>
              <Row label="Treatment" value={r.treatment} />
              <Row label="Prescription" value={r.prescription} />
              <Row label="Notes" value={r.notes} />
            </dl>
          </article>
        ))}

      {data && <Pager page={data.page} totalPages={data.totalPages} total={data.totalElements} onPage={setPage} />}

      {form && (
        <RecordForm
          patientId={patient.id}
          record={form === 'new' ? undefined : form}
          onClose={() => setForm(null)}
          onDone={() => {
            setForm(null)
            reload()
          }}
        />
      )}
      {history && <HistoryDialog record={history} onClose={() => setHistory(null)} />}
    </>
  )
}
