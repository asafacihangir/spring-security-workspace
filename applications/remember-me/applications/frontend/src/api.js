// Thin fetch wrapper around the backend API.
//
// No JWT/token layer here on purpose: this app authenticates with a
// session cookie (JSESSIONID / remember-me cookie), so the browser
// attaches credentials automatically as long as we ask it to.
const BASE_URL = '/api'

// CSRF (final-review fix-round): SecurityConfig enables CSRF protection via
// CookieCsrfTokenRepository.withHttpOnlyFalse() - the XSRF-TOKEN cookie is
// deliberately NOT HttpOnly (unlike JSESSIONID/the remember-me cookie) so
// this script can read it and echo it back as the X-XSRF-TOKEN header,
// which is what CsrfFilter actually checks against the cookie value. Any
// GET request causes the backend's CsrfCookieFilter to (re-)issue this
// cookie if it isn't already present - see that class's javadoc - so by the
// time a user submits the login form, at least one GET (App.jsx's initial
// /api/auth-status call) has already happened and the cookie exists.
function readCookie(name) {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1]) : null
}

// Only POST/PUT/DELETE need the CSRF header - GET requests are not
// state-changing and CsrfFilter never checks them.
function csrfHeaders() {
  const token = readCookie('XSRF-TOKEN')
  return token ? { 'X-XSRF-TOKEN': token } : {}
}

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
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', ...csrfHeaders() },
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
    headers: { ...csrfHeaders() },
  })
  return response
}

// JSON request helper for the notes CRUD endpoints (UC-006).
async function apiJson(path, method, body) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
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
    headers: { ...csrfHeaders() },
  })
  return response
}
