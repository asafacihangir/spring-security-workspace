// Thin fetch wrapper around the backend API.
//
// No JWT/token layer here on purpose: this app authenticates with a
// session cookie (JSESSIONID / remember-me cookie), so the browser
// attaches credentials automatically as long as we ask it to.
const BASE_URL = '/api'

export async function apiGet(path) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'GET',
    credentials: 'same-origin',
  })
  return response
}

// Posts a plain x-www-form-urlencoded body, the shape Spring Security's
// UsernamePasswordAuthenticationFilter expects from a login submission.
export async function apiPostForm(path, fields) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(fields),
  })
  return response
}

// Bodiless POST, used for /logout (UC-003): Spring Security's LogoutFilter
// only cares about the request method/URL, not a body.
export async function apiPost(path) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    credentials: 'same-origin',
  })
  return response
}

// JSON request helper for the notes CRUD endpoints (UC-006).
async function apiJson(path, method, body) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return response
}

export async function apiPostJson(path, body) {
  return apiJson(path, 'POST', body)
}

export async function apiPutJson(path, body) {
  return apiJson(path, 'PUT', body)
}

export async function apiDelete(path) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'DELETE',
    credentials: 'same-origin',
  })
  return response
}
