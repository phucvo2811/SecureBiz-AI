import type { PageResponse } from './organization'

export interface Role { id: string; code: string; name: string; description?: string; systemRole: boolean; status: string; permissions: string[] }
export interface Permission { id: string; code: string; name: string; module: string; description?: string; status: string; sensitive: boolean }
export interface User { id: string; username: string; email: string; status: string; accountType: string; roles: string[]; directPermissions: string[]; effectivePermissions: string[]; activeSessionCount: number; lastLoginAt?: string; authorizationVersion: number }
export type UserPage = PageResponse<User>
