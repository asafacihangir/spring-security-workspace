import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './store/AuthContext'
import AppRoutes from './routes'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
