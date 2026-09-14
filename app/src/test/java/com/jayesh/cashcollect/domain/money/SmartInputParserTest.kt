package com.jayesh.cashcollect.domain.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SmartInputParserTest {

    @Test
    fun `parses party name and shorthand 400 as 4 lakh`() {
        val result = SmartInputParser.parse("sambhu 400")
        assertNotNull(result)
        assertEquals("sambhu", result!!.customerName)
        assertEquals(40_000_000L, result.amountPaise) // 4,00,000 rupees
        assertEquals("₹4,00,000", result.formattedRupees)
    }

    @Test
    fun `parses 100 as 1 lakh`() {
        val paise = SmartInputParser.parseAmountToken("100")
        assertEquals(10_000_000L, paise) // 1,00,000 rupees
    }

    @Test
    fun `parses 500 as 5 lakh`() {
        val paise = SmartInputParser.parseAmountToken("500")
        assertEquals(50_000_000L, paise) // 5,00,000 rupees
    }

    @Test
    fun `parses 5000 as 50 lakh`() {
        val paise = SmartInputParser.parseAmountToken("5000")
        assertEquals(500_000_000L, paise) // 50,00,000 rupees
    }

    @Test
    fun `parses 1L and 1_5l correctly`() {
        assertEquals(10_000_000L, SmartInputParser.parseAmountToken("1l"))
        assertEquals(15_000_000L, SmartInputParser.parseAmountToken("1.5L"))
        assertEquals(500_000_000L, SmartInputParser.parseAmountToken("50lakh"))
    }

    @Test
    fun `parses 20k and 50k correctly`() {
        assertEquals(2_000_000L, SmartInputParser.parseAmountToken("20k"))
        assertEquals(5_000_000L, SmartInputParser.parseAmountToken("50k"))
    }

    @Test
    fun `parses full rupee amount 20000 correctly`() {
        assertEquals(2_000_000L, SmartInputParser.parseAmountToken("20000"))
    }

    @Test
    fun `parses multi-word customer name with shorthand amount`() {
        val result = SmartInputParser.parse("Mahesh Bhai Angadia 1.5L")
        assertNotNull(result)
        assertEquals("Mahesh Bhai Angadia", result!!.customerName)
        assertEquals(15_000_000L, result.amountPaise)
    }
}
