import { useEffect, useState } from 'react'
import { apiGet, apiPost } from './api'
import LoginForm from './LoginForm'
import NotesPage from './NotesPage'

function App() {
  const [username, setUsername] = useState(null)
  const [checkingSession, setCheckingSession] = useState(true)

  useEffect(() => {
    let cancelled = false
    apiGet('/me')
      .then(async (response) => {
        if (cancelled) return
        if (response.ok) {
          const body = await response.json()
          setUsername(body.username)
        }
      })
      .finally(() => {
        if (!cancelled) setCheckingSession(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  async function handleLoginSuccess() {
    const response = await apiGet('/me')
    const body = await response.json()
    setUsername(body.username)
  }

  // UC-003: the backend has already invalidated the session and cleared
  // both cookies by the time this resolves. Since this is an SPA (no
  // server-rendered login page to redirect to), "kullanıcıyı login
  // sayfasına yönlendirir" means dropping back to the LoginForm branch
  // below by clearing the known username.
  async function handleLogout() {
    await apiPost('/logout')
    setUsername(null)
  }

  if (checkingSession) {
    return null
  }

  return username ? (
    <NotesPage username={username} onLogout={handleLogout} />
  ) : (
    <LoginForm onLoginSuccess={handleLoginSuccess} />
  )
}

export default App
