import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { Alert, Field } from '../components/ui.jsx'
import { BrandMark } from '../components/Layout.jsx'

// Fictional accounts created by the backend's demo data (see the backend README).
const DEMO = [
  { label: 'Administrator', email: 'admin@hospital.test', password: 'Admin@12345' },
  { label: 'Receptionist', email: 'reception@hospital.test', password: 'Password@123' },
  { label: 'Doctor (Dr. Rao)', email: 'dr.rao@hospital.test', password: 'Password@123' },
  { label: 'Doctor (Dr. Iyer)', email: 'dr.iyer@hospital.test', password: 'Password@123' },
  { label: 'Nurse', email: 'nurse.priya@hospital.test', password: 'Password@123' },
  { label: 'Patient', email: 'asha.verma@patient.test', password: 'Password@123' },
]

export default function Login() {
  const { user, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(email, password)
    } catch (err) {
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
          <h1>Every view and every edit is on the record.</h1>
          <p>Sign in with your hospital account. Staff see what their role allows. Patients can book and manage their own appointments.</p>
        </div>
        <p className="small-text">Demonstration system with fictional data only.</p>
      </section>

      <section className="login-panel">
        <h2>Sign in</h2>
        {error && <Alert kind="error">{error}</Alert>}
        <form onSubmit={submit}>
          <Field label="Email">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
              required
              autoFocus
            />
          </Field>
          <Field label="Password">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </Field>
          <button type="submit" className="btn primary" disabled={busy}>
            {busy ? 'Signing in' : 'Sign in'}
          </button>
        </form>

        <p className="auth-switch">
          New patient? <Link to="/register">Create your account</Link>
        </p>

        <div className="demo">
          <p>Demo accounts. Choose one to fill in the form.</p>
          <div className="demo-buttons">
            {DEMO.map((d) => (
              <button
                key={d.email}
                type="button"
                className="btn small"
                onClick={() => {
                  setEmail(d.email)
                  setPassword(d.password)
                }}
              >
                {d.label}
              </button>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}

