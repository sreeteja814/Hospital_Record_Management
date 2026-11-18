import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, getToken, setToken, setUnauthorizedHandler } from './api.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false)

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  // On page load: if a token is saved from this tab, find out who it belongs to.
  useEffect(() => {
    if (!getToken()) {
      setReady(true)
      return
    }
    api('GET', '/api/auth/me')
      .then(setUser)
      .catch(() => setToken(null))
      .finally(() => setReady(true))
  }, [])

  const login = useCallback(async (email, password) => {
    const result = await api('POST', '/api/auth/login', { email, password }, { auth: false })
    setToken(result.token)
    const me = await api('GET', '/api/auth/me')
    setUser(me)
    return me
  }, [])

  const value = useMemo(() => ({ user, ready, login, logout }), [user, ready, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
