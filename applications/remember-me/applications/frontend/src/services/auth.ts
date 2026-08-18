import { apiFetch } from './api'

export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  roles: string[]
}

export interface SignupData {
  firstName: string
  lastName: string
  email: string
  password: string
}

export async function fetchMe(): Promise<User | null> {
  const response = await fetch('/api/auth/me')
  return response.ok ? response.json() : null
}

export async function login(
  username: string,
  password: string,
  rememberMe: boolean,
): Promise<User> {
  const body = new URLSearchParams({ username, password })
  if (rememberMe) body.set('remember-me', 'on')
  // redirect: 'manual' — Spring'in 302'si takip edilmez (Location backend origin'ini
  // gösterip CORS hatası üretir); cookie'ler 302 yanıtında zaten set edilir.
  await fetch('/login', { method: 'POST', body, redirect: 'manual' })
  const me = await fetchMe()
  if (!me) throw new Error('Invalid username or password')
  return me
}

export async function logout(): Promise<void> {
  await fetch('/logout', { method: 'POST', redirect: 'manual' })
}

export async function signup(data: SignupData): Promise<User> {
  return apiFetch<User>('/api/signup', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}
