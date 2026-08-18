import { createContext, useCallback, useEffect, useState, type ReactNode } from 'react'
import * as authService from '../services/auth'
import type { User } from '../services/auth'

export interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (username: string, password: string, rememberMe: boolean) => Promise<void>
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    setUser(await authService.fetchMe())
  }, [])

  useEffect(() => {
    refresh().finally(() => setLoading(false))
  }, [refresh])

  const login = useCallback(
    async (username: string, password: string, rememberMe: boolean) => {
      setUser(await authService.login(username, password, rememberMe))
    },
    [],
  )

  const logout = useCallback(async () => {
    await authService.logout()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}
