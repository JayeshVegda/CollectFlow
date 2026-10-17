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

    @Test
    fun `toCompactRupees and toShortRupeeString format compact Indian currency`() {
        val tenK = Paise.fromRupees(10_000L)
        assertEquals("₹10,000", tenK.toCompactRupees())
        assertEquals("₹10,000", tenK.toShortRupeeString())

        val fourLakh = Paise.fromRupees(400_000L)
        assertEquals("₹4L", fourLakh.toCompactRupees())
        assertEquals("₹4L", fourLakh.toShortRupeeString())

        val oneAndHalfLakh = Paise.fromRupees(150_000L)
        assertEquals("₹1.5L", oneAndHalfLakh.toCompactRupees())
        assertEquals("₹1.5L", oneAndHalfLakh.toShortRupeeString())

        val twoCrore = Paise.fromRupees(20_000_000L)
        assertEquals("₹2Cr", twoCrore.toCompactRupees())
        assertEquals("₹2Cr", twoCrore.toShortRupeeString())
    }
}
