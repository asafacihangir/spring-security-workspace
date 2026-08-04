import { useState } from 'react'
import { apiPostForm } from './api'

// UC-001 login form: username, password, error message.
//
// A2 (missing fields): caught client-side before any request is sent.
// A1 (wrong credentials): the backend always replies with one generic
// message (BR-002 - never which field was wrong); on failure the password
// field is cleared per the use case's alternative flow.
function LoginForm({ onLoginSuccess }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()

    if (!username || !password) {
      setError('Kullanıcı adı ve şifre zorunludur.')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      const response = await apiPostForm('/login', { username, password })
      if (response.ok) {
        onLoginSuccess()
        return
      }
      setError('Kullanıcı adı veya şifre hatalı.')
      setPassword('')
    } catch {
      setError('İstek gönderilemedi, lütfen tekrar deneyin.')
      setPassword('')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <h1>Giriş Yap</h1>
      <div>
        <label htmlFor="username">Kullanıcı adı</label>
        <input
          id="username"
          type="text"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          autoComplete="username"
        />
      </div>
      <div>
        <label htmlFor="password">Şifre</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
        />
      </div>
      {error && <p role="alert">{error}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Giriş yapılıyor...' : 'Giriş Yap'}
      </button>
    </form>
  )
}

export default LoginForm
