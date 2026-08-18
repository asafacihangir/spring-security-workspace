import { useContext } from 'react'
import { AuthContext } from '../store/AuthContext'

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  const isAdmin = context.user?.roles.includes('ROLE_ADMIN') ?? false
  return { ...context, isAdmin }
}
