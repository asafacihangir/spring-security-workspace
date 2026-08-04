import { useState } from 'react'
import { apiPostJson } from './api'

// UC-009: re-authentication upgrades a Remembered session to Fully
// Authenticated by re-checking the CURRENT principal's own password
// (BR-012 - the backend never trusts a username from this form; there is
// no username field to send at all, see ReauthenticateRequest).
//
// A1 (wrong password): generic-ish error, level unchanged - the form stays
// up so the user can try again (use case continues at step 2).
// A2 (user navigates away instead): needs no special handling here - the
// "Vazgeç" button just calls onCancel, and simply not submitting the form
// at all (browser back, closing the tab) already leaves the level exactly
// as Remembered, since nothing on the backend changes until a successful
// POST /api/reauthenticate.
function ReauthPage({ onSuccess, onCancel }) {
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()

    if (!password) {
      setError('Şifre zorunludur.')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      const response = await apiPostJson('/reauthenticate', { password })
      if (response.ok) {
        onSuccess()
        return
      }
      setError('Şifre doğrulanamadı.')
      setPassword('')
    } catch {
      setError('İstek gönderilemedi, lütfen tekrar deneyin.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <h1>Yeniden Doğrulama</h1>
      <p>Hesap Ayarları&apos;na erişmek için şifrenizi yeniden girin.</p>
      <div>
        <label htmlFor="reauth-password">Şifre</label>
        <input
          id="reauth-password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
        />
      </div>
      {error && <p role="alert">{error}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Doğrulanıyor...' : 'Doğrula'}
      </button>
      <button type="button" onClick={onCancel}>
        Vazgeç
      </button>
    </form>
  )
}

export default ReauthPage
