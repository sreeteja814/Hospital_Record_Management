import { useEffect, useState } from 'react'
import { api, qs } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useFetch } from '../hooks.js'
import { ROLE_LABEL } from '../roles.js'
import { Alert, Empty, ErrorNote, Field, Loading, Modal, PageHead, Pager, Tag } from '../components/ui.jsx'

function StaffForm({ onClose, onDone }) {
  const [v, setV] = useState({ fullName: '', email: '', password: '', role: 'RECEPTIONIST', specialization: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const set = (key) => (e) => setV({ ...v, [key]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    try {
      await api('POST', '/api/admin/staff', {
        fullName: v.fullName,
        email: v.email,
        password: v.password,
        role: v.role,
        specialization: v.role === 'DOCTOR' ? v.specialization : null,
      })
      onDone()
    } catch (err) {
      const fe = err.fieldErrors || {}
      setFieldErrors(fe)
      setError(Object.keys(fe).length ? 'Check the fields marked below.' : err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title="Add staff account" onClose={onClose}>
      {error && <Alert kind="error">{error}</Alert>}
      <form onSubmit={submit} noValidate>
        <Field label="Full name" error={fieldErrors.fullName}>
          <input value={v.fullName} onChange={set('fullName')} maxLength={100} autoFocus />
        </Field>
        <Field label="Email" error={fieldErrors.email}>
          <input type="email" value={v.email} onChange={set('email')} autoComplete="off" />
        </Field>
        <Field label="Temporary password" error={fieldErrors.password} hint="At least 8 characters.">
          <input type="password" value={v.password} onChange={set('password')} autoComplete="new-password" />
        </Field>
        <Field label="Role" error={fieldErrors.role}>
          <select value={v.role} onChange={set('role')}>
            {Object.entries(ROLE_LABEL).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </Field>
        {v.role === 'DOCTOR' && (
          <Field label="Specialization" error={fieldErrors.specialization}>
            <input value={v.specialization} onChange={set('specialization')} maxLength={100} />
          </Field>
        )}
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn primary" disabled={busy}>
            {busy ? 'Saving' : 'Add staff account'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default function Staff() {
  const { user } = useAuth()
  const [role, setRole] = useState('')
  const [page, setPage] = useState(0)
  const [adding, setAdding] = useState(false)
  const [actionError, setActionError] = useState(null)

  useEffect(() => {
    setPage(0)
  }, [role])

  const { data, error, loading, reload } = useFetch(
    () => api('GET', `/api/admin/staff${qs({ role, page, size: 15 })}`),
    [role, page],
  )

  async function setActive(member, active) {
    const text = active
      ? `Reactivate ${member.fullName}?`
      : `Deactivate ${member.fullName}? They will be signed out and cannot sign in again until reactivated.`
    if (!window.confirm(text)) return
    setActionError(null)
    try {
      await api('PATCH', `/api/admin/staff/${member.id}/active`, { active })
      reload()
    } catch (err) {
      setActionError(err.message)
    }
  }

  return (
    <>
      <PageHead title="Staff" subtitle="Accounts for people who can sign in. There is no public sign-up.">
        <button type="button" className="btn primary" onClick={() => setAdding(true)}>
          Add staff account
        </button>
      </PageHead>

      <div className="filters">
        <label className="field">
          <span className="field-label">Role</span>
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="">All roles</option>
            {Object.entries(ROLE_LABEL).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {actionError && <Alert kind="error">{actionError}</Alert>}
      <ErrorNote error={error} />

      <div className="table-wrap">
        {loading && !data ? (
          <Loading />
        ) : data && data.content.length === 0 ? (
          <Empty title="No staff accounts found">Try a different role.</Empty>
        ) : (
          data && (
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.content.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <strong>{m.fullName}</strong>
                      {m.specialization && <span className="sub">{m.specialization}</span>}
                    </td>
                    <td>{m.email}</td>
                    <td>{ROLE_LABEL[m.role]}</td>
                    <td>{m.active ? <Tag tone="green">Active</Tag> : <Tag>Deactivated</Tag>}</td>
                    <td className="actions">
                      {m.id === user.id ? (
                        <span className="muted small-text">You</span>
                      ) : m.active ? (
                        <button type="button" className="btn small danger" onClick={() => setActive(m, false)}>
                          Deactivate
                        </button>
                      ) : (
                        <button type="button" className="btn small" onClick={() => setActive(m, true)}>
                          Reactivate
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

      {adding && (
        <StaffForm
          onClose={() => setAdding(false)}
          onDone={() => {
            setAdding(false)
            reload()
          }}
        />
      )}
    </>
  )
}
