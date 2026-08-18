import { Link } from 'react-router-dom'

export default function ForbiddenPage() {
  return (
    <>
      <h1>Access Denied</h1>
      <p>
        You do not have permission to view this page. Try logging in as{' '}
        <code>admin1@example.com</code> / <code>admin1</code>.
      </p>
      <Link to="/login">Go to Login</Link>
    </>
  )
}
