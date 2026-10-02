export interface ApiFieldError {
  field: string
  code: string
  message: string
}

export interface ApiError {
  timestamp: string
  status: number
  code: string
  message: string
  correlationId?: string
  errors: ApiFieldError[]
}
