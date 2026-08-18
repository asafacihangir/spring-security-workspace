import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, type FieldErrors as FieldErrorMap } from '../../services/api'
import { createWorkLog } from '../../services/worklogs'
import { ErrorAlert, FieldError } from '../../components/FieldErrors'

export default function WorkLogForm() {
  const navigate = useNavigate()
  const [explanation, setExplanation] = useState('')
  const [errors, setErrors] = useState<FieldErrorMap>({})
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setErrors({})
    setError(null)
    try {
      await createWorkLog({ explanation })
      navigate('/work-logs/my')
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setErrors(err.fieldErrors)
      } else {
        setError('Failed to create the work log.')
      }
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <ErrorAlert message={error} />
      <div className="mb-3">
        <label htmlFor="explanation" className="form-label">
          Explanation
        </label>
        <textarea
          id="explanation"
          rows={5}
          className={`form-control${errors.explanation ? ' is-invalid' : ''}`}
          value={explanation}
          onChange={(e) => setExplanation(e.target.value)}
        />
        <FieldError errors={errors} field="explanation" />
      </div>
      <button type="submit" className="btn btn-primary">
        Create
      </button>
    </form>
  )
}
