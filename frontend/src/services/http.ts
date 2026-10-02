import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

export const http = axios.create({
  baseURL,
  timeout: 15_000,
  withCredentials: true,
  headers: { Accept: 'application/json' },
})

export const refreshHttp = axios.create({
  baseURL,
  timeout: 15_000,
  withCredentials: true,
  headers: { Accept: 'application/json', 'X-Requested-With': 'SecureBiz' },
})
