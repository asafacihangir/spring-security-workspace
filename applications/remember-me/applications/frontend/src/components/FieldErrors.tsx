import type { FieldErrors as FieldErrorMap } from '../services/api'

export function FieldError({ errors, field }: { errors: FieldErrorMap; field: string }) {
  const message = errors[field]
  return message ? <div className="invalid-feedback d-block">{message}</div> : null
}

export function ErrorAlert({ message }: { message: string | null }) {
  return message ? <div className="alert alert-danger">{message}</div> : null
}
