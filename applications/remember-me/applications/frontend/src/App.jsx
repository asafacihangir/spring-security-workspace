import { useEffect, useState } from 'react'
import { apiGet, apiPost } from './api'
import LoginForm from './LoginForm'
import NotesPage from './NotesPage'
import AuthIndicator from './AuthIndicator'
import AccountSettingsPage from './AccountSettingsPage'
import ReauthPage from './ReauthPage'
import TokenInspectorPage from './TokenInspectorPage'

// UC-007/008/009 (Faz 5): `authLevel` is this app's single piece of state
// for the Anonymous/Remembered/Fully Authenticated indicator (BR-009),
// always sourced from GET /api/auth-status - never inferred locally from
// "did login just succeed" or similar, so it can never drift from what the
// backend actually thinks. `view` picks which screen is visible once a
// username is known: the notes list, Account Settings (UC-008), or the
// re-authentication form (UC-009).
//
// UC-015/BR-022 (Faz 9): `rememberMeParameterName` is the same idea applied
// to the remember-me opt-in field name - sourced from the same
// GET /api/auth-status response (see AuthStatusController on the backend)
// rather than hardcoded here or in LoginForm, so the frontend can never send
// a different name than what SecurityConfig's rememberMe() DSL is actually
// listening for. 'remember-me' as the initial state is only a placeholder
// until the first fetch resolves (Spring's own default - UC-015 A1) and is
// never relied on as the source of truth itself.
function App() {
  const [username, setUsername] = useState(null)
  const [authLevel, setAuthLevel] = useState('ANONYMOUS')
  const [rememberMeParameterName, setRememberMeParameterName] = useState('remember-me')
  const [checkingSession, setCheckingSession] = useState(true)
  const [view, setView] = useState('notes')

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const level = await refreshAuthLevel()
      if (!cancelled && level !== 'ANONYMOUS') {
        const response = await apiGet('/me')
        if (!cancelled && response.ok) {
          const body = await response.json()
          setUsername(body.username)
        }
      }
      if (!cancelled) setCheckingSession(false)
    })()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function refreshAuthLevel() {
    const response = await apiGet('/auth-status')
    const body = response.ok ? await response.json() : null
    const level = body ? body.level : 'ANONYMOUS'
    setAuthLevel(level)
    // UC-015 A1: if the request fails outright, keep whatever name is
    // already in state (itself defaulted to Spring's own 'remember-me')
    // rather than overwriting it with something derived from a failed
    // response.
    if (body && body.rememberMeParameter) {
      setRememberMeParameterName(body.rememberMeParameter)
    }
    return level
  }

  async function handleLoginSuccess() {
    const response = await apiGet('/me')
    const body = await response.json()
    setUsername(body.username)
    setView('notes')
    await refreshAuthLevel()
  }

  // UC-003: the backend has already invalidated the session and cleared
  // both cookies by the time this resolves. Since this is an SPA (no
  // server-rendered login page to redirect to), "kullanıcıyı login
  // sayfasına yönlendirir" means dropping back to the LoginForm branch
  // below by clearing the known username.
  async function handleLogout() {
    await apiPost('/logout')
    setUsername(null)
    setView('notes')
    await refreshAuthLevel()
  }

  // UC-008 A1: Account Settings itself (not this button) is what actually
  // decides whether the caller is let in - see AccountSettingsPage, which
  // reacts to a 401/403 from the backend by calling onNeedsReauth.
  function openAccountSettings() {
    setView('account')
  }

  function needsReauth() {
    setView('reauth')
  }

  // UC-009 main scenario step 5: back to the page that sent the user here.
  async function handleReauthSuccess() {
    await refreshAuthLevel()
    setView('account')
  }

  // UC-013 (Faz 7): reachable whether or not a username is known - GET
  // /api/token-inspector is permitAll (see TokenInspectorController's
  // javadoc), so this view is available from the login screen too, not just
  // once already logged in.
  function openTokenInspector() {
    setView('token-inspector')
  }

  if (checkingSession) {
    return null
  }

  let content
  if (view === 'token-inspector') {
    content = <TokenInspectorPage onBack={() => setView('notes')} />
  } else if (!username) {
    content = (
      <>
        <LoginForm
          rememberMeParameterName={rememberMeParameterName}
          onLoginSuccess={handleLoginSuccess}
        />
        <p>
          <button type="button" onClick={openTokenInspector}>
            Token Inspector
          </button>
        </p>
      </>
    )
  } else if (view === 'account') {
    content = <AccountSettingsPage onNeedsReauth={needsReauth} onBack={() => setView('notes')} />
  } else if (view === 'reauth') {
    content = <ReauthPage onSuccess={handleReauthSuccess} onCancel={() => setView('notes')} />
  } else {
    content = (
      <NotesPage
        username={username}
        onLogout={handleLogout}
        onOpenAccountSettings={openAccountSettings}
        onOpenTokenInspector={openTokenInspector}
      />
    )
  }

  return (
    <>
      <AuthIndicator level={authLevel} />
      {content}
    </>
  )
}

export default App
