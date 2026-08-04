// UC-007: always-visible Anonymous / Remembered / Fully Authenticated
// indicator (BR-009 - exactly one level shown, and it is whatever App.jsx's
// single `authLevel` state currently holds, itself sourced from
// GET /api/auth-status, the backend's single source of truth). This
// component does no fetching or auth logic of its own - it only renders
// whatever level it is given.
const LABELS = {
  ANONYMOUS: 'Anonymous',
  REMEMBERED: 'Remembered',
  FULLY_AUTHENTICATED: 'Fully Authenticated',
}

function AuthIndicator({ level }) {
  return (
    <p>
      Auth seviyesi: <strong>{LABELS[level] ?? level}</strong>
    </p>
  )
}

export default AuthIndicator
