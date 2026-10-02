import { describe, expect, it } from 'vitest'
import { calculateDiscount } from './pricing'

describe('POS discount calculation', () => {
  it('calculates a percentage discount in VND', () => expect(calculateDiscount(11_980_000, 10, 'PERCENT')).toBe(1_198_000))
  it('caps percentage discounts at 100 percent', () => expect(calculateDiscount(500_000, 150, 'PERCENT')).toBe(500_000))
  it('caps fixed discounts at the subtotal', () => expect(calculateDiscount(500_000, 800_000, 'AMOUNT')).toBe(500_000))
  it('does not allow a negative discount', () => expect(calculateDiscount(500_000, -10, 'AMOUNT')).toBe(0))
})
