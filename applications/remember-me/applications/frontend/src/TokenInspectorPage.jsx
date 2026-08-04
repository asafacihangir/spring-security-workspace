import { useEffect, useState } from 'react'
import { apiGet } from './api'

// UC-013 (Faz 7): read-only view onto GET /api/token-inspector, so UC-011's
// token rotation (BR-015/016) and UC-012's stolen-cookie detection
// (BR-017/018) can actually be watched happen instead of taken on faith.
//
// BR-019 (freshness): no polling and no client-side caching of the previous
// response - mount and every "Yenile" click both do a plain fetch, so the
// table can never show anything older than the single most recent request.
// TokenInspectorController.java's own javadoc covers the backend half of
// this guarantee (no query cache either).
//
// A1/A2: both driven off the response's `strategy` field, never inferred
// from `records` being empty - see TokenInspectorResponse.java for why an
// empty list alone can't tell the two apart.
function TokenInspectorPage({ onBack }) {
  const [strategy, setStrategy] = useState(null)
  const [records, setRecords] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    try {
      const response = await apiGet('/token-inspector')
      if (response.ok) {
        const body = await response.json()
        setStrategy(body.strategy)
        setRecords(body.records)
        setError(null)
      } else {
        setError('Kayıtlar yüklenemedi.')
      }
    } catch {
      setError('Kayıtlar yüklenemedi.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  return (
    <main>
      <h1>Token Inspector</h1>
      <p>
        <button type="button" onClick={load} disabled={loading}>
          Yenile
        </button>{' '}
        <button type="button" onClick={onBack}>
          Geri
        </button>
      </p>

      {loading && <p>Yükleniyor...</p>}
      {error && <p role="alert">{error}</p>}

      {!loading && !error && strategy && strategy !== 'PERSISTENT' && (
        // A2: token-based strategy active - an empty table here would look
        // like a bug rather than an intentional "wrong mode", so this tells
        // the learner what to change instead of leaving them guessing.
        <p>
          Şu anda token-based remember-me stratejisi aktif; sunucu tarafında izlenecek
          kalıcı bir kayıt tutulmuyor. Bu sayfayı kullanmak için{' '}
          <code>app.remember-me.strategy=persistent</code> yapıp backend'i yeniden
          başlat (bkz. UC-010).
        </p>
      )}

      {!loading && !error && strategy === 'PERSISTENT' && records.length === 0 && (
        // A1: persistent mode is active, but no record exists yet.
        <p>
          Henüz bir kalıcı hatırlanma kaydı yok. "Remember Me" seçeneğiyle giriş
          yaparak bir kayıt oluşturabilirsin.
        </p>
      )}

      {!loading && !error && strategy === 'PERSISTENT' && records.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Kullanıcı Adı</th>
              <th>Series</th>
              <th>Token</th>
              <th>Son Kullanım</th>
            </tr>
          </thead>
          <tbody>
            {records.map((record) => (
              <tr key={record.series}>
                <td>{record.username}</td>
                <td>{record.series}</td>
                <td>{record.token}</td>
                <td>{record.lastUsed}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  )
}

export default TokenInspectorPage
