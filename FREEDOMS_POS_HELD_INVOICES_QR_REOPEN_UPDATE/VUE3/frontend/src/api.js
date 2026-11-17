import axios from 'axios'

export const api = axios.create({
  baseURL: '/',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: { 'X-Requested-With': 'XMLHttpRequest', Accept: 'application/json' }
})

export async function loadCsrf() {
  await api.get('/api/auth/csrf')
}

export async function apiLogin(email, password) {
  await loadCsrf()
  const form = new URLSearchParams({ email, password })
  await api.post('/login', form, { headers: { 'Content-Type': 'application/x-www-form-urlencoded', Accept: 'application/json' } })
  await loadCsrf()
  return (await api.get('/api/auth/me')).data
}

export async function apiLogout() {
  await loadCsrf()
  await api.post('/logout')
  await loadCsrf()
}

export function errorMessage(error, fallback = 'Có lỗi xảy ra. Vui lòng thử lại.') {
  return error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback
}
