import { http } from './http'
import type { Permission, Role, User, UserPage } from '@/types/identity'

export interface AuthSession {
  id: string
  deviceName?: string
  ipAddress?: string
  userAgent?: string
  startedAt: string
  lastSeenAt: string
  expiresAt: string
  revokedAt?: string
  revokeReason?: string
  current: boolean
}

export async function getUsers(search = '', page = 0, size = 20) {
  return (await http.get<UserPage>('/users', { params: { search, page, size } })).data
}
export async function getRoles() { return (await http.get<Role[]>('/roles')).data }
export async function getPermissions() { return (await http.get<Permission[]>('/permissions')).data }
export interface CreateUserInput { username:string;email:string;password:string;status:string;accountType:string;roleIds:string[];permissionIds:string[];reason?:string }
export async function createUser(input: CreateUserInput) {
  return (await http.post<User>('/users', input)).data
}
export async function updateUserRoles(id:string,roleIds:string[]){return(await http.put<User>(`/users/${id}/roles`,{roleIds})).data}
export async function getUser(id:string){return(await http.get<User>(`/users/${id}`)).data}
export async function updateUserPermissions(id:string,permissionIds:string[],reason:string){return(await http.put<User>(`/users/${id}/permissions`,{permissionIds,reason})).data}
export async function lockUser(id:string){return(await http.post<User>(`/users/${id}/lock`)).data}
export async function unlockUser(id:string){return(await http.post<User>(`/users/${id}/unlock`)).data}
export async function revokeUserSessions(id:string){await http.post(`/users/${id}/sessions/revoke`)}
export async function updateRolePermissions(id:string,permissionIds:string[]){return(await http.put<Role>(`/roles/${id}/permissions`,{permissionIds})).data}
export async function createRole(input:{code:string;name:string;description?:string;permissionIds:string[]}){return(await http.post<Role>('/roles',input)).data}
export async function updateRole(id:string,input:{name:string;description?:string}){return(await http.put<Role>(`/roles/${id}`,input)).data}
export async function deactivateRole(id:string){await http.post(`/roles/${id}/deactivate`)}
export async function getMySessions(){return(await http.get<AuthSession[]>('/auth/sessions')).data}
export async function revokeMySession(id:string){await http.delete(`/auth/sessions/${id}`)}
