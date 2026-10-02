export const ROLE_LABELS:Record<string,string>={OWNER:'Chủ doanh nghiệp',IT_ADMIN:'Quản trị Công nghệ & AI',MANAGER:'Quản lý',SALES:'Nhân viên bán hàng',WAREHOUSE:'Nhân viên kho',ACCOUNTANT:'Kế toán',HR:'Nhân sự',ADMIN:'Quản trị viên cũ'}
const IT_CONSOLE_PERMISSIONS=['system.health.view','security.view','ai.model.view','integration.view','audit.view','user.view','session.view']
export function canAccessItConsole(has:(permission:string)=>boolean){return IT_CONSOLE_PERMISSIONS.some(has)}
export function canEditDirectPermissions(has:(permission:string)=>boolean){return has('user.permission.assign')}
export function effectivePermissionPreview(rolePermissions:Iterable<string>,directPermissions:Iterable<string>){return new Set([...rolePermissions,...directPermissions])}
