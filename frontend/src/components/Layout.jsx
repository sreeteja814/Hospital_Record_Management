import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { ROLE_LABEL } from '../roles.js'

export function BrandMark() {
  return (
    <svg className="brand-mark" viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="6" fill="#fff" />
      <path d="M13 7h6v6h6v6h-6v6h-6v-6H7v-6h6z" fill="#0e3b3a" />
    </svg>
  )
}

export default function Layout() {
  const { user, logout } = useAuth()

  const patients = { to: '/patients', label: 'Patients' }
  const appointments = {
    to: '/appointments',
    label: user.role === 'DOCTOR' ? 'My schedule' : 'Appointments',
  }
  let links = user.role === 'DOCTOR' ? [appointments, patients] : [patients, appointments]
  if (user.role === 'PATIENT') links = [{ to: '/my-appointments', label: 'My appointments' }]
  if (user.role === 'ADMIN') {
    links.push({ to: '/staff', label: 'Staff' }, { to: '/audit', label: 'Audit log' })
  }

  return (
    <div className="shell">
      <aside className="side">
        <div className="brand">
          <BrandMark />
          <span>Hospital Records</span>
        </div>
        <nav aria-label="Main">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} className={({ isActive }) => (isActive ? 'active' : '')}>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="me" data-role={user.role}>
          <strong>{user.fullName}</strong>
          <span>{ROLE_LABEL[user.role]}</span>
          <button type="button" className="btn small ghost" onClick={logout}>
            Sign out
          </button>
        </div>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
