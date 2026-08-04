import { useState } from 'react'
import { apiPostForm } from './api'

// UC-001 login form: username, password, error message.
// UC-002 adds the "Remember Me" checkbox (BR-003: opt-in only - the
// `remember-me` field is only sent at all when the box is checked, so an
// unchecked submission looks to the backend exactly like a request that
// never mentioned remember-me, and Spring Security's rememberMe() filter
// issues no cookie).
//
// A2 (missing fields): caught client-side before any request is sent.
// A1 (wrong credentials): the backend always replies with one generic
// message (BR-002 - never which field was wrong); on failure the password
// field is cleared per the use case's alternative flow. UC-002's A2 (bad
// credentials while remember-me is checked) falls out of this for free:
// form-login authentication fails before Spring Security's remember-me
// filter ever runs, so nothing is produced either way.
function LoginForm({ onLoginSuccess }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
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
      const fields = { username, password }
      if (rememberMe) {
        // Matches Spring Security's default remember-me parameter name
        // (see SecurityConfig.REMEMBER_ME_PARAMETER on the backend).
        fields['remember-me'] = 'true'
      }
      const response = await apiPostForm('/login', fields)
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
      <div>
        <label htmlFor="remember-me">
          <input
            id="remember-me"
            type="checkbox"
            checked={rememberMe}
            onChange={(event) => setRememberMe(event.target.checked)}
          />
          Beni hatırla
        </label>
      </div>
      {error && <p role="alert">{error}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Giriş yapılıyor...' : 'Giriş Yap'}
      </button>
    </form>
  )
}

export default LoginForm
