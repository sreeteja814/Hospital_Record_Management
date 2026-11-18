const dateFmt = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
const timeFmt = new Intl.DateTimeFormat('en-IN', { hour: 'numeric', minute: '2-digit', hour12: true })

// The backend sends hospital-local times without a time zone ("2026-11-05T10:00:00").
// Read them as local time so 10:00 always shows as 10:00.
function parseLocal(value) {
  const m = /^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?/.exec(value || '')
  if (!m) return null
  return new Date(+m[1], +m[2] - 1, +m[3], +(m[4] || 0), +(m[5] || 0))
}

export function fmtDate(value) {
  const d = parseLocal(value)
  return d ? dateFmt.format(d) : ''
}

export function fmtTime(value) {
  const d = parseLocal(value)
  return d ? timeFmt.format(d) : ''
}

// Instants (with a trailing Z, such as audit times and record timestamps) are shown in the viewer's time zone.
export function fmtInstant(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return `${dateFmt.format(d)}, ${timeFmt.format(d)}`
}

export function ageFrom(dateOfBirth) {
  const d = parseLocal(dateOfBirth)
  if (!d) return ''
  const now = new Date()
  let age = now.getFullYear() - d.getFullYear()
  const birthdayPassed =
    now.getMonth() > d.getMonth() || (now.getMonth() === d.getMonth() && now.getDate() >= d.getDate())
  if (!birthdayPassed) age -= 1
  return age
}

const pad = (n) => String(n).padStart(2, '0')

export function todayISO() {
  const n = new Date()
  return `${n.getFullYear()}-${pad(n.getMonth() + 1)}-${pad(n.getDate())}`
}

// Value for <input type="datetime-local" min="...">
export function nowLocalInput() {
  const n = new Date()
  return `${todayISO()}T${pad(n.getHours())}:${pad(n.getMinutes())}`
}

// Empty text boxes become null: the backend validates optional fields like blood group only when present.
export function clean(values) {
  return Object.fromEntries(
    Object.entries(values).map(([k, v]) => [k, typeof v === 'string' && v.trim() === '' ? null : v]),
  )
}
