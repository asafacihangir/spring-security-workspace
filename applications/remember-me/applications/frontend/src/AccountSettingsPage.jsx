import { useEffect, useState } from 'react'
import { apiGet, apiPutJson } from './api'

// UC-008 Account Settings. BR-010/BR-011's actual enforcement is the
// backend's isFullyAuthenticated() rule on /api/account (SecurityConfig) -
// this component only reacts to that: a 401/403 here means "go
// re-authenticate" (A1), nothing more. It is not itself the access-control
// mechanism, only a UX nicety on top of one.
function AccountSettingsPage({ onNeedsReauth, onBack }) {
  const [displayName, setDisplayName] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    let cancelled = false
    apiGet('/account')
      .then(async (response) => {
        if (cancelled) return
        if (response.status === 401 || response.status === 403) {
          onNeedsReauth()
          return
        }
        if (response.ok) {
          const body = await response.json()
          setDisplayName(body.displayName ?? '')
        } else {
          setError('Hesap bilgileri yüklenemedi.')
        }
      })
      .catch(() => {
        if (!cancelled) setError('Hesap bilgileri yüklenemedi.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [onNeedsReauth])

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    setSaved(false)
    try {
      const response = await apiPutJson('/account', { displayName })
      if (response.status === 401 || response.status === 403) {
        onNeedsReauth()
        return
      }
      if (response.ok) {
        setSaved(true)
      } else {
        setError('Kaydedilemedi, lütfen tekrar deneyin.')
      }
    } catch {
      setError('İstek gönderilemedi, lütfen tekrar deneyin.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <p>Yükleniyor...</p>
  }

  return (
    <main>
      <h1>Hesap Ayarları</h1>
      <button type="button" onClick={onBack}>
        Notlara Dön
      </button>
      <form onSubmit={handleSubmit} noValidate>
        <div>
          <label htmlFor="display-name">Görünen ad</label>
          <input
            id="display-name"
            type="text"
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
          />
        </div>
        {error && <p role="alert">{error}</p>}
        {saved && <p>Kaydedildi.</p>}
        <button type="submit" disabled={saving}>
          {saving ? 'Kaydediliyor...' : 'Kaydet'}
        </button>
      </form>
    </main>
  )
}

export default AccountSettingsPage
