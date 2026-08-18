import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

export default function WelcomePage() {
  const { user, isAdmin } = useAuth()
  return (
    <>
      <h1>Welcome to myCalendar!</h1>
      <p>
        This chapter demonstrates <strong>Remember-Me Services</strong> with Spring
        Security. Log in with the "Remember me" box checked, delete the{' '}
        <code>JSESSIONID</code> cookie, and reload — you will still be logged in.
      </p>
      <ul>
        {isAdmin && (
          <li>
            <Link to="/work-logs">All Work Logs</Link>
          </li>
        )}
        {user && (
          <>
            <li>
              <Link to="/work-logs/my">My Work Logs</Link>
            </li>
            <li>
              <Link to="/work-logs/new">Create Work Log</Link>
            </li>
          </>
        )}
        {!user && (
          <li>
            <Link to="/login">Login</Link>
          </li>
        )}
      </ul>
    </>
  )
}
