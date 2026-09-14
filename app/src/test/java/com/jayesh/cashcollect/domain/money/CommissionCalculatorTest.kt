package com.jayesh.cashcollect.domain.money

import org.junit.Assert.assertEquals
import org.junit.Test

class CommissionCalculatorTest {

    @Test
    fun `exact calculation of Rs 4,00,000 at 3 per thousand yields Rs 1,200`() {
        // Rs 4,00,000 = 40,000,000 paise
        val amountPaise = 40_000_000L
        val commission = CommissionCalculator.calculate(amountPaise, 3)

        // Expected: 120,000 paise = Rs 1,200
        assertEquals(120_000L, commission)
        assertEquals("₹1,200", Paise(commission).toFormattedRupees())
    }

    @Test
    fun `round-half-up rounds up when remainder is half or more`() {
        // (100 * 3 + 500) / 1000 = 800 / 1000 = 0
        assertEquals(0L, CommissionCalculator.calculate(100L, 3))

        // (167 * 3 + 500) / 1000 = (501 + 500) / 1000 = 1001 / 1000 = 1
        assertEquals(1L, CommissionCalculator.calculate(167L, 3))

        // 500 paise (Rs 5) at rate 3 -> (500 * 3 + 500) / 1000 = 2000 / 1000 = 2 paise
        assertEquals(2L, CommissionCalculator.calculate(500L, 3))
    }

    @Test
    fun `zero amount yields zero commission`() {
        assertEquals(0L, CommissionCalculator.calculate(0L, 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative amount throws exception`() {
        CommissionCalculator.calculate(-100L, 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative rate throws exception`() {
        CommissionCalculator.calculate(1000L, -1)
    }
}
