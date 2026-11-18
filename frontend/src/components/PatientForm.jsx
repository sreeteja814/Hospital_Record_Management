import { useState } from 'react'
import { Alert, Field, Modal } from './ui.jsx'
import { clean } from '../format.js'

const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']

// Used both to register a new patient and to edit an existing one.
// onSave receives the cleaned values and must return a promise; the parent closes the dialog on success.
export default function PatientForm({ title, initial, submitLabel, onSave, onClose }) {
  const [v, setV] = useState({
    fullName: initial?.fullName ?? '',
    dateOfBirth: initial?.dateOfBirth ?? '',
    gender: initial?.gender ?? '',
    phone: initial?.phone ?? '',
    email: initial?.email ?? '',
    address: initial?.address ?? '',
    bloodGroup: initial?.bloodGroup ?? '',
    emergencyContactName: initial?.emergencyContactName ?? '',
    emergencyContactPhone: initial?.emergencyContactPhone ?? '',
  })
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
      await onSave(clean(v))
    } catch (err) {
      const fe = err.fieldErrors || {}
      setFieldErrors(fe)
      setError(Object.keys(fe).length ? 'Check the fields marked below.' : err.message)
      setBusy(false)
    }
  }

  return (
    <Modal title={title} onClose={onClose} wide>
      {error && <Alert kind="error">{error}</Alert>}
      <form onSubmit={submit} noValidate>
        <div className="form-grid">
          <Field label="Full name" error={fieldErrors.fullName} className="wide">
            <input value={v.fullName} onChange={set('fullName')} maxLength={100} autoFocus />
          </Field>
          <Field label="Date of birth" error={fieldErrors.dateOfBirth}>
            <input type="date" value={v.dateOfBirth} onChange={set('dateOfBirth')} max={new Date().toISOString().slice(0, 10)} />
          </Field>
          <Field label="Gender" error={fieldErrors.gender}>
            <select value={v.gender} onChange={set('gender')}>
              <option value="">Select</option>
              <option value="FEMALE">Female</option>
              <option value="MALE">Male</option>
              <option value="OTHER">Other</option>
            </select>
          </Field>
          <Field label="Phone" error={fieldErrors.phone} hint="7 to 20 digits. Spaces, + and - are fine.">
            <input value={v.phone} onChange={set('phone')} inputMode="tel" />
          </Field>
          <Field label="Blood group (optional)" error={fieldErrors.bloodGroup}>
            <select value={v.bloodGroup} onChange={set('bloodGroup')}>
              <option value="">Not recorded</option>
              {BLOOD_GROUPS.map((g) => (
                <option key={g} value={g}>
                  {g}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Email (optional)" error={fieldErrors.email} className="wide">
            <input type="email" value={v.email} onChange={set('email')} />
          </Field>
          <Field label="Address (optional)" error={fieldErrors.address} className="wide">
            <input value={v.address} onChange={set('address')} maxLength={255} />
          </Field>
          <Field label="Emergency contact name (optional)" error={fieldErrors.emergencyContactName}>
            <input value={v.emergencyContactName} onChange={set('emergencyContactName')} maxLength={100} />
          </Field>
          <Field label="Emergency contact phone (optional)" error={fieldErrors.emergencyContactPhone}>
            <input value={v.emergencyContactPhone} onChange={set('emergencyContactPhone')} inputMode="tel" />
          </Field>
        </div>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn primary" disabled={busy}>
            {busy ? 'Saving' : submitLabel}
          </button>
        </div>
      </form>
    </Modal>
  )
}
