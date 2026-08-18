import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import MainLayout from '../layouts/MainLayout'
import WelcomePage from '../pages/home/WelcomePage'
import LoginPage from '../pages/auth/LoginPage'
import SignupPage from '../pages/auth/SignupPage'
import AllWorkLogsPage from '../pages/worklogs/AllWorkLogsPage'
import MyWorkLogsPage from '../pages/worklogs/MyWorkLogsPage'
import WorkLogDetailPage from '../pages/worklogs/WorkLogDetailPage'
import CreateWorkLogPage from '../pages/worklogs/CreateWorkLogPage'
import ForbiddenPage from '../pages/errors/ForbiddenPage'

function RequireAuth() {
  const { user, loading } = useAuth()
  if (loading) return null
  return user ? <Outlet /> : <Navigate to="/login" replace />
}

function RequireAdmin() {
  const { isAdmin } = useAuth()
  return isAdmin ? <Outlet /> : <Navigate to="/403" replace />
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<WelcomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/403" element={<ForbiddenPage />} />
        <Route element={<RequireAuth />}>
          <Route path="/work-logs/my" element={<MyWorkLogsPage />} />
          <Route path="/work-logs/new" element={<CreateWorkLogPage />} />
          <Route path="/work-logs/:workLogId" element={<WorkLogDetailPage />} />
          <Route element={<RequireAdmin />}>
            <Route path="/work-logs" element={<AllWorkLogsPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
