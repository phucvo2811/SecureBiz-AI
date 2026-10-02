import { describe, expect, it, vi } from 'vitest'
import { authorizeNavigation, type GuardRoute, type RouteAuthState } from './guard'

function route(overrides: Partial<GuardRoute> = {}): GuardRoute {
  return { fullPath: '/inventory', name: 'inventory', meta: {}, ...overrides }
}

function auth(overrides: Partial<RouteAuthState> = {}): RouteAuthState {
  return {
    initialized: true,
    isAuthenticated: true,
    initialize: vi.fn(async () => undefined),
    hasPermission: vi.fn(() => true),
    ...overrides,
  }
}

describe('authorizeNavigation', () => {
  it('initializes session then preserves the requested redirect for anonymous users', async () => {
    const state=auth({ initialized:false,isAuthenticated:false,initialize:vi.fn(async()=>undefined) })
    await expect(authorizeNavigation(route(),state)).resolves.toEqual({name:'login',query:{redirect:'/inventory'}})
    expect(state.initialize).toHaveBeenCalledOnce()
  })

  it('moves an authenticated user away from the login screen', async () => {
    await expect(authorizeNavigation(route({fullPath:'/login',name:'login',meta:{public:true}}),auth()))
      .resolves.toEqual({name:'dashboard'})
  })

  it('rejects a route when the required permission is absent', async () => {
    const state=auth({hasPermission:vi.fn(()=>false)})
    await expect(authorizeNavigation(route({meta:{permissions:['inventory.view']}}),state))
      .resolves.toEqual({name:'forbidden'})
  })

  it('accepts any one permission for mixed workflow screens', async () => {
    const state=auth({hasPermission:vi.fn((permission)=>permission==='return.approve')})
    await expect(authorizeNavigation(route({meta:{permissions:['return.create','return.approve'],permissionMode:'any'}}),state))
      .resolves.toBe(true)
  })

  it('blocks the IT console without a technical permission', async () => {
    const state=auth({hasPermission:vi.fn(()=>false)})
    await expect(authorizeNavigation(route({fullPath:'/it/overview',name:'it-overview',meta:{permissions:['system.health.view']}}),state))
      .resolves.toEqual({name:'forbidden'})
  })

  it('allows the IT console with system health permission', async () => {
    const state=auth({hasPermission:vi.fn(permission=>permission==='system.health.view')})
    await expect(authorizeNavigation(route({fullPath:'/it/overview',name:'it-overview',meta:{permissions:['system.health.view']}}),state))
      .resolves.toBe(true)
  })
})
