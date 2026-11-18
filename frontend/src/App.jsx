import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Patients from './pages/Patients.jsx'
import PatientDetail from './pages/PatientDetail.jsx'
import Appointments from './pages/Appointments.jsx'
import Staff from './pages/Staff.jsx'
import Audit from './pages/Audit.jsx'
import Register from './pages/Register.jsx'
import MyAppointments from './pages/MyAppointments.jsx'
import { STAFF_ROLES } from './roles.js'

// Shows the app shell only to signed-in users (and only to the listed roles, if any).
function Protected({ roles }) {
  const { user, ready } = useAuth()
  if (!ready) return <div className="boot">Loading</div>
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return <Layout />
}

function Home() {
  const { user } = useAuth()
  const target =
    user.role === 'PATIENT' ? '/my-appointments' : user.role === 'DOCTOR' ? '/appointments' : '/patients'
  return <Navigate to={target} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<Protected />}>
        <Route index element={<Home />} />
      </Route>
      <Route element={<Protected roles={STAFF_ROLES} />}>
        <Route path="patients" element={<Patients />} />
        <Route path="patients/:id" element={<PatientDetail />} />
        <Route path="appointments" element={<Appointments />} />
      </Route>
      <Route element={<Protected roles={['PATIENT']} />}>
        <Route path="my-appointments" element={<MyAppointments />} />
      </Route>
      <Route element={<Protected roles={['ADMIN']} />}>
        <Route path="staff" element={<Staff />} />
        <Route path="audit" element={<Audit />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
