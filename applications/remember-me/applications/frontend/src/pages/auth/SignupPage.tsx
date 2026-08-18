import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, type FieldErrors as FieldErrorMap } from '../../services/api'
import { signup } from '../../services/auth'
import { useAuth } from '../../hooks/useAuth'
import { ErrorAlert, FieldError } from '../../components/FieldErrors'

export default function SignupPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' })
  const [errors, setErrors] = useState<FieldErrorMap>({})
  const [error, setError] = useState<string | null>(null)

  const setField = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [field]: e.target.value })

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setErrors({})
    setError(null)
    try {
      await signup(form)
      // Backend signup'ta oturum açmaz; aynı bilgilerle client-side login yapılır.
      await login(form.email, form.password, false)
      navigate('/')
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setErrors(err.fieldErrors)
      } else {
        setError('Signup failed.')
      }
    }
  }

  const fields = [
    { name: 'firstName', label: 'First Name', type: 'text' },
    { name: 'lastName', label: 'Last Name', type: 'text' },
    { name: 'email', label: 'Email', type: 'email' },
    { name: 'password', label: 'Password', type: 'password' },
  ] as const

  return (
    <div className="col-md-6 offset-md-3">
      <h1>Signup</h1>
      <ErrorAlert message={error} />
      <form onSubmit={handleSubmit} noValidate>
        {fields.map(({ name, label, type }) => (
          <div className="mb-3" key={name}>
            <label htmlFor={name} className="form-label">
              {label}
            </label>
            <input
              id={name}
              type={type}
              className={`form-control${errors[name] ? ' is-invalid' : ''}`}
              value={form[name]}
              onChange={setField(name)}
            />
            <FieldError errors={errors} field={name} />
          </div>
        ))}
        <button type="submit" className="btn btn-primary">
          Create Account
        </button>
      </form>
    </div>
  )
}
