export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Department {
  id: string
  code: string
  name: string
  description?: string
  parentId?: string
  managerId?: string
  status: 'ACTIVE' | 'INACTIVE'
  version: number
  createdAt: string
  updatedAt: string
}

export interface Employee {
  id: string
  employeeCode: string
  fullName: string
  phone?: string
  email?: string
  departmentId: string
  departmentName: string
  position: string
  hireDate: string
  terminationDate?: string
  status: 'ACTIVE' | 'ON_LEAVE' | 'TERMINATED'
  userId?: string
  version: number
}
