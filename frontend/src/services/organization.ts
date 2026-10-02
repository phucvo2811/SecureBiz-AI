import { http } from './http'
import type { Department, Employee, PageResponse } from '@/types/organization'

export async function getDepartments(search = '', page = 0, size = 20) {
  const { data } = await http.get<PageResponse<Department>>('/departments', { params: { search, page, size } })
  return data
}

export async function getEmployees(search = '', page = 0, size = 20) {
  const { data } = await http.get<PageResponse<Employee>>('/employees', { params: { search, page, size } })
  return data
}

export async function createDepartment(input: {
  code: string; name: string; description?: string; status: 'ACTIVE' | 'INACTIVE'
}) {
  const { data } = await http.post<Department>('/departments', input)
  return data
}

export async function createEmployee(input: {
  employeeCode: string; fullName: string; email?: string; departmentId: string
  position: string; hireDate: string; status: 'ACTIVE' | 'ON_LEAVE' | 'TERMINATED'
}) {
  const { data } = await http.post<Employee>('/employees', input)
  return data
}
