const currency = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

const number = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 4 })

const statusLabels: Record<string, string> = {
  ACTIVE: 'Đang hoạt động', INACTIVE: 'Ngừng hoạt động', DISCONTINUED: 'Ngừng kinh doanh',
  PENDING: 'Chờ xử lý', DRAFT: 'Bản nháp', SUBMITTED: 'Đã gửi duyệt', APPROVED: 'Đã duyệt',
  CANCELLED: 'Đã hủy', COMPLETED: 'Hoàn tất', PARTIALLY_RETURNED: 'Đã trả một phần', RETURNED: 'Đã trả hàng',
  PAID: 'Đã thanh toán', UNPAID: 'Chưa thanh toán', PARTIALLY_REFUNDED: 'Đã hoàn một phần', REFUNDED: 'Đã hoàn tiền',
  CASH: 'Tiền mặt', CARD: 'Thẻ', BANK_TRANSFER: 'Chuyển khoản', E_WALLET: 'Ví điện tử',
  OPEN: 'Chưa xử lý', INVESTIGATING: 'Đang điều tra', RESOLVED: 'Đã giải quyết', FALSE_POSITIVE: 'Không phải rủi ro',
  LOW: 'Thấp', MEDIUM: 'Trung bình', HIGH: 'Cao', CRITICAL: 'Nghiêm trọng',
  ON_LEAVE: 'Đang nghỉ', TERMINATED: 'Đã nghỉ việc', PUBLIC: 'Công khai', INTERNAL: 'Nội bộ',
  CONFIDENTIAL: 'Bảo mật', RESTRICTED: 'Hạn chế', RESTOCK: 'Nhập lại kho', DAMAGED: 'Hàng hỏng',
  QUARANTINE: 'Chờ kiểm tra', PURCHASE_RECEIPT: 'Nhập hàng', SALE: 'Bán hàng', RETURN: 'Trả hàng',
  TRANSFER_OUT: 'Chuyển kho đi', TRANSFER_IN: 'Chuyển kho đến', ADJUSTMENT: 'Điều chỉnh', STOCKTAKE: 'Kiểm kho',
}

export function formatMoney(value: number | string | null | undefined) {
  return currency.format(Number(value ?? 0))
}

export function formatNumber(value: number | string | null | undefined) {
  return number.format(Number(value ?? 0))
}

export function formatDateTime(value?: string) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
}

export function formatDate(value?: string) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short' }).format(new Date(value))
}

export function statusLabel(value?: string) {
  if (!value) return '—'
  return statusLabels[value] ?? value.replaceAll('_', ' ').toLocaleLowerCase('vi-VN')
}

export function statusTone(value?: string) {
  if (!value) return 'neutral'
  if (['ACTIVE', 'APPROVED', 'COMPLETED', 'PAID', 'RESOLVED', 'RESTOCK'].includes(value)) return 'success'
  if (['PENDING', 'DRAFT', 'SUBMITTED', 'INVESTIGATING', 'MEDIUM', 'ON_LEAVE'].includes(value)) return 'warning'
  if (['INACTIVE', 'CANCELLED', 'TERMINATED', 'DAMAGED', 'CRITICAL', 'HIGH'].includes(value)) return 'danger'
  if (['LOW', 'OPEN', 'PARTIALLY_RETURNED', 'PARTIALLY_REFUNDED'].includes(value)) return 'info'
  return 'neutral'
}
