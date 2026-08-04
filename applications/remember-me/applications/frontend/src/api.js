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
