const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export const SESSION_ID = (() => {
  let id = sessionStorage.getItem('advisor_session')
  if (!id) {
    id = crypto.randomUUID()
    sessionStorage.setItem('advisor_session', id)
  }
  return id
})()

export async function apiFetch(path, options = {}, token = null) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  if (options.body instanceof FormData) {
    delete headers['Content-Type']
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (res.status === 401) {
    localStorage.removeItem('fiscal_token')
    localStorage.removeItem('fiscal_user')
    window.location.href = '/login'
    return
  }

  // Limită de tokens atinsă — anunță aplicația să arate ecranul de upgrade.
  if (res.status === 402) {
    window.dispatchEvent(new CustomEvent('quota-exceeded'))
  }

  if (res.status === 204) return null

  const text = await res.text()
  let data
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = text
  }

  if (!res.ok) {
    const err = new Error(
      (data && (data.message || data.error)) || `Eroare ${res.status}`
    )
    err.status = res.status
    err.data = data
    throw err
  }

  return data
}
