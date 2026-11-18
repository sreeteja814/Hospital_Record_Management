// Small fetch wrapper for the Spring Boot API.
// - adds the JWT as "Authorization: Bearer ..."
// - turns the backend's JSON errors ({ detail, errors }) into an ApiError with a readable message
// - signs the user out when the server says the token is no longer valid (401)

const TOKEN_KEY = 'hospital.token'

let onUnauthorized = () => {}

export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

// sessionStorage: the token disappears when the browser tab is closed.
export function getToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token) {
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token)
    else sessionStorage.removeItem(TOKEN_KEY)
  } catch {
    // storage unavailable: the user will simply have to sign in again after a reload
  }
}

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors || {}
  }
}

const FALLBACK = {
  400: 'The request was not valid.',
  401: 'Your session has ended. Sign in again.',
  403: 'You do not have permission to do this.',
  404: 'That item was not found.',
  409: 'That conflicts with something that already exists.',
}

export async function api(method, path, body, { auth = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (auth && token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check that the backend is running on port 8080.')
  }

  if (res.status === 204) return null

  let data = null
  const text = await res.text()
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = null
    }
  }

  if (!res.ok) {
    if (res.status === 401 && auth) onUnauthorized()
    const message = (data && data.detail) || FALLBACK[res.status] || `Something went wrong (error ${res.status}).`
    throw new ApiError(res.status, message, data && data.errors)
  }
  return data
}

// Builds "?a=1&b=2", skipping empty values.
export function qs(params) {
  const p = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.set(k, v)
  })
  const s = p.toString()
  return s ? `?${s}` : ''
}
