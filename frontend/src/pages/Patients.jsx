import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, qs } from '../api.js'
import { useAuth } from '../auth.jsx'
import { useDebounced, useFetch } from '../hooks.js'
import { isFrontDesk } from '../roles.js'
import { ageFrom, fmtDate } from '../format.js'
import { Empty, ErrorNote, Loading, PageHead, Pager, Tag } from '../components/ui.jsx'
import PatientForm from '../components/PatientForm.jsx'

const GENDER = { FEMALE: 'Female', MALE: 'Male', OTHER: 'Other' }

export default function Patients() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [deactivated, setDeactivated] = useState(false)
  const [page, setPage] = useState(0)
  const [registering, setRegistering] = useState(false)
  const search = useDebounced(q, 300)

  useEffect(() => {
    setPage(0)
  }, [search, deactivated])

  const { data, error, loading } = useFetch(
    () => api('GET', `/api/patients${qs({ q: search, active: !deactivated, page, size: 10 })}`),
    [search, deactivated, page],
  )

  async function register(values) {
    const created = await api('POST', '/api/patients', values)
    navigate(`/patients/${created.id}`)
  }

  return (
    <>
      <PageHead title="Patients" subtitle="Search by name, medical record number or phone.">
        {isFrontDesk(user.role) && (
          <button type="button" className="btn primary" onClick={() => setRegistering(true)}>
            Register patient
          </button>
        )}
      </PageHead>

      <div className="filters">
        <label className="field grow">
          <span className="field-label">Search</span>
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Name, MRN or phone"
            autoFocus
          />
        </label>
        <label className="check">
          <input type="checkbox" checked={deactivated} onChange={(e) => setDeactivated(e.target.checked)} />
          Show deactivated patients only
        </label>
      </div>

      <ErrorNote error={error} />

      <div className="table-wrap">
        {loading && !data ? (
          <Loading />
        ) : data && data.content.length === 0 ? (
          <Empty title={deactivated ? 'No deactivated patients' : 'No patients found'}>
            {!deactivated && isFrontDesk(user.role) ? 'Try a different search, or register a new patient.' : 'Try a different search.'}
          </Empty>
        ) : (
          data && (
            <table>
              <thead>
                <tr>
                  <th>Patient</th>
                  <th>MRN</th>
                  <th>Date of birth</th>
                  <th>Gender</th>
                  <th>Phone</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <Link to={`/patients/${p.id}`}>
                        <strong>{p.fullName}</strong>
                      </Link>
                      {!p.active && (
                        <>
                          {' '}
                          <Tag>Deactivated</Tag>
                        </>
                      )}
                    </td>
                    <td>
                      <span className="mrn">{p.mrn}</span>
                    </td>
                    <td>
                      {fmtDate(p.dateOfBirth)}
                      <span className="sub">{ageFrom(p.dateOfBirth)} years</span>
                    </td>
                    <td>{GENDER[p.gender] || p.gender}</td>
                    <td>{p.phone}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        )}
      </div>

      {data && <Pager page={data.page} totalPages={data.totalPages} total={data.totalElements} onPage={setPage} />}

      {registering && (
        <PatientForm
          title="Register patient"
          submitLabel="Register patient"
          onSave={register}
          onClose={() => setRegistering(false)}
        />
      )}
    </>
  )
}
