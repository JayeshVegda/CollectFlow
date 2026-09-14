package com.jayesh.cashcollect.domain.money

import org.junit.Assert.assertEquals
import org.junit.Test

class PaiseTest {

    @Test
    fun `formatting formats large numbers in Indian format`() {
        val paise = Paise.fromRupees(400000L) // 4,00,000
        assertEquals("₹4,00,000", paise.toFormattedRupees())

        val smallPaise = Paise.fromPaise(150L) // 1.50
        assertEquals("₹1.50", smallPaise.toFormattedRupees())

        val tenLakh = Paise.fromRupees(1000000L) // 10,00,000
        assertEquals("₹10,00,000", tenLakh.toFormattedRupees())
    }

    @Test
    fun `arithmetic operations add and subtract paise correctly`() {
        val a = Paise(500L)
        val b = Paise(200L)
        assertEquals(Paise(700L), a + b)
        assertEquals(Paise(300L), a - b)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative paise creation throws exception`() {
        Paise(-1L)
    }
}
