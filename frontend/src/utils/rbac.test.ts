import {describe,expect,it,vi} from 'vitest'
import {ROLE_LABELS,canAccessItConsole,canEditDirectPermissions,effectivePermissionPreview} from './rbac'
describe('RBAC presentation',()=>{
 it('shows IT mode with any supported technical permission',()=>{expect(canAccessItConsole(vi.fn(p=>p==='security.view'))).toBe(true);expect(canAccessItConsole(vi.fn(()=>false))).toBe(false)})
 it('shows the direct permission editor only to its authority',()=>{expect(canEditDirectPermissions(vi.fn(p=>p==='user.permission.assign'))).toBe(true);expect(canEditDirectPermissions(vi.fn(()=>false))).toBe(false)})
 it('unions role and direct permissions without duplicates',()=>{expect([...effectivePermissionPreview(['product.view','order.view'],['order.view','inventory.view'])].sort()).toEqual(['inventory.view','order.view','product.view'])})
 it('uses Vietnamese role labels',()=>{expect(ROLE_LABELS.IT_ADMIN).toBe('Quản trị Công nghệ & AI');expect(ROLE_LABELS.OWNER).toBe('Chủ doanh nghiệp')})
})
