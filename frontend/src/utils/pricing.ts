export type DiscountMode = 'AMOUNT' | 'PERCENT'

export function calculateDiscount(subtotal: number, value: number, mode: DiscountMode): number {
  const safeSubtotal = Math.max(0, Number(subtotal) || 0)
  const safeValue = Math.max(0, Number(value) || 0)
  const raw = mode === 'PERCENT'
    ? safeSubtotal * Math.min(safeValue, 100) / 100
    : safeValue
  return Math.min(safeSubtotal, Math.round(raw))
}
