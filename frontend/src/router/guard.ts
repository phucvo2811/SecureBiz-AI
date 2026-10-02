export interface RouteAuthState {
  initialized: boolean
  isAuthenticated: boolean
  initialize: () => Promise<void>
  hasPermission: (permission: string) => boolean
}

export interface GuardRoute {
  fullPath: string
  name?: unknown
  meta: {
    public?: boolean
    permissions?: string[]
    permissionMode?: 'all' | 'any'
  }
}

export async function authorizeNavigation(to: GuardRoute, auth: RouteAuthState) {
  if (!auth.initialized) await auth.initialize()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthenticated) return { name: 'dashboard' }
  const required = to.meta.permissions ?? []
  if (auth.isAuthenticated && required.length > 0) {
    const allowed = to.meta.permissionMode === 'all'
      ? required.every(auth.hasPermission)
      : required.some(auth.hasPermission)
    if (!allowed) return { name: 'forbidden' }
  }
  return true
}
