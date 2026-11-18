import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Alert, Field } from '../components/ui.jsx'
import { BrandMark } from '../components/Layout.jsx'

// A patient creates their own login. The hospital checks the MRN and date of birth against the patient record.
export default function Register() {
  const { user, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [mrn, setMrn] = useState('')
  const [dateOfBirth, setDateOfBirth] = useState('')
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    setBusy(true)
    try {
      await api('POST', '/api/auth/register', { email, password, mrn: mrn.trim(), dateOfBirth }, { auth: false })
      await login(email, password) // signs in and the app moves on to "My appointments"
    } catch (err) {
      setFieldErrors(err.fieldErrors || {})
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <section className="login-intro">
        <div className="brand">
          <BrandMark />
          <span>Hospital Records</span>
        </div>
        <div>
          <h1>Book your own appointments.</h1>
          <p>
            Create a login with the medical record number (MRN) from your registration card and your date of birth.
            You will be able to book, view and cancel your own appointments. Your medical records stay with your
            doctors.
          </p>
        </div>
        <p className="small-text">Not registered yet? The reception desk can create your patient record first.</p>
      </section>

      <section className="login-panel">
        <h2>Create a patient account</h2>
        {error && <Alert kind="error">{error}</Alert>}
        <form onSubmit={submit} noValidate>
          <Field label="MRN" error={fieldErrors.mrn} hint="Printed on your registration card, like MRN-3F9A12BC.">
            <input value={mrn} onChange={(e) => setMrn(e.target.value)} autoComplete="off" required autoFocus />
          </Field>
          <Field label="Date of birth" error={fieldErrors.dateOfBirth}>
            <input type="date" value={dateOfBirth} onChange={(e) => setDateOfBirth(e.target.value)} required />
          </Field>
          <Field label="Email" error={fieldErrors.email}>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
              required
            />
          </Field>
          <Field label="Password" error={fieldErrors.password} hint="At least 8 characters.">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
              minLength={8}
              required
            />
          </Field>
          <button type="submit" className="btn primary" disabled={busy}>
            {busy ? 'Creating account' : 'Create account'}
          </button>
        </form>
        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </section>
    </div>
  )
}
