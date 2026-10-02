import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { http, refreshHttp } from '@/services/http'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'

export interface AuthenticatedUser {
  id: string
  username: string
  email: string
  permissions: string[]
}

interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: AuthenticatedUser
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthenticatedUser | null>(null)
  const accessToken = ref<string | null>(null)
  const initialized = ref(false)
  let refreshPromise: Promise<AuthResponse> | null = null
  let interceptorInstalled = false
  const isAuthenticated = computed(() => user.value !== null)
  const hasPermission = (permission: string) => user.value?.permissions.includes(permission) ?? false

  function applySession(response: AuthResponse) {
    user.value = response.user
    accessToken.value = response.accessToken
    http.defaults.headers.common.Authorization = `Bearer ${response.accessToken}`
  }

  async function login(identifier: string, password: string, deviceName?: string) {
    const { data } = await http.post<AuthResponse>('/auth/login', { identifier, password, deviceName })
    applySession(data)
  }

  async function initialize() {
    installResponseInterceptor()
    if (initialized.value) return
    try {
      const { data } = await refreshHttp.post<AuthResponse>('/auth/refresh')
      applySession(data)
    } catch {
      clearSession()
    } finally {
      initialized.value = true
    }
  }

  async function logout() {
    try {
      await refreshHttp.post('/auth/logout')
    } finally {
      clearSession()
    }
  }

  function clearSession() {
    user.value = null
    accessToken.value = null
    delete http.defaults.headers.common.Authorization
  }

  function installResponseInterceptor() {
    if (interceptorInstalled) return
    interceptorInstalled = true
    http.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const config = error.config as (InternalAxiosRequestConfig & { _secureBizRetried?: boolean }) | undefined
        if (error.response?.status !== 401 || !config || config._secureBizRetried || config.url?.startsWith('/auth/')) {
          return Promise.reject(error)
        }
        config._secureBizRetried = true
        try {
          if (!refreshPromise) {
            refreshPromise = refreshHttp.post<AuthResponse>('/auth/refresh').then(({ data }) => data)
              .finally(() => { refreshPromise = null })
          }
          const refreshed = await refreshPromise
          applySession(refreshed)
          config.headers.Authorization = `Bearer ${refreshed.accessToken}`
          return http.request(config)
        } catch (refreshError) {
          clearSession()
          return Promise.reject(refreshError)
        }
      },
    )
  }

  return { user, initialized, isAuthenticated, hasPermission, login, initialize, logout, clearSession }
})
