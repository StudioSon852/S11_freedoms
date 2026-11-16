import { defineStore } from 'pinia'
import { api, apiLogin, apiLogout, loadCsrf, errorMessage } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({ user: null, loaded: false, busy: false, error: '' }),
  getters: {
    isAuthenticated: state => Boolean(state.user),
    role: state => state.user?.role || 'GUEST',
    isAdmin: state => ['ADMIN', 'MANAGER'].includes(state.user?.role)
  },
  actions: {
    async loadMe() {
      if (this.loaded) return this.user
      try {
        const response = await api.get('/api/auth/me')
        this.user = response.data
      } catch {
        this.user = null
      } finally {
        this.loaded = true
      }
      return this.user
    },
    async login(email, password) {
      this.busy = true
      this.error = ''
      try {
        this.user = await apiLogin(email, password)
        this.loaded = true
        return this.user
      } catch (error) {
        this.error = errorMessage(error, 'Không đăng nhập được.')
        throw error
      } finally {
        this.busy = false
      }
    },
    async logout() {
      await apiLogout()
      this.user = null
      this.loaded = true
    },
    async register(payload) {
      this.busy = true
      try {
        await loadCsrf()
        return (await api.post('/api/auth/register', payload)).data
      } catch (error) {
        this.error = errorMessage(error)
        throw error
      } finally {
        this.busy = false
      }
    }
  }
})
