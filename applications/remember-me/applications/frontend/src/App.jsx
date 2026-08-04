import { useState } from 'react'
import { apiGet } from './api'

function App() {
  const [result, setResult] = useState(null)

  async function checkBackendHealth() {
    setResult('checking...')
    try {
      const response = await apiGet('/health')
      const body = await response.text()
      setResult(`HTTP ${response.status} - ${body}`)
    } catch (error) {
      setResult(`request failed: ${error.message}`)
    }
  }

  return (
    <main>
      <h1>remember-me lab</h1>
      <p>Faz 0 iskeleti: backend'e proxy'li basit bir istek at.</p>
      <button type="button" onClick={checkBackendHealth}>
        Check backend health
      </button>
      {result && <pre>{result}</pre>}
    </main>
  )
}

export default App
