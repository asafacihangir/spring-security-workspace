import { useEffect, useState } from 'react'
import { apiGet } from './api'
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

  if (checkingSession) {
    return null
  }

  return username ? (
    <NotesPage username={username} />
  ) : (
    <LoginForm onLoginSuccess={handleLoginSuccess} />
  )
}

export default App
