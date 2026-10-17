package com.jayesh.cashcollect.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvImporterTest {

    @Test
    fun `parseCsvRows handles basic comma separation and quoted cells with commas`() {
        val csv = "ID,Customer Name,Amount\n1,\"Ramesh, Bhai\",400000\n2,Suresh,200000"
        val rows = parseCsvRows(csv)
        assertEquals(3, rows.size)
        assertEquals(listOf("ID", "Customer Name", "Amount"), rows[0])
        assertEquals(listOf("1", "Ramesh, Bhai", "400000"), rows[1])
        assertEquals(listOf("2", "Suresh", "200000"), rows[2])
    }

    @Test
    fun `parseCsvRows handles escaped quotes within fields`() {
        val csv = "ID,Note\n1,\"Collected from \"\"godown\"\"\"\n"
        val rows = parseCsvRows(csv)
        assertEquals(2, rows.size)
        assertEquals("Collected from \"godown\"", rows[1][1])
    }

    @Test
    fun `parseCsvRows handles windows CRLF line breaks`() {
        val csv = "ID,Name\r\n1,Sambhu\r\n2,Naveen\r\n"
        val rows = parseCsvRows(csv)
        assertEquals(3, rows.size)
        assertEquals(listOf("ID", "Name"), rows[0])
        assertEquals(listOf("1", "Sambhu"), rows[1])
        assertEquals(listOf("2", "Naveen"), rows[2])
    }
}
