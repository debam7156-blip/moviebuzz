package com.moviebuzz

import com.moviebuzz.app.data.import.CsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvParserTest {

    @Test
    fun `parses quoted fields with commas and escaped quotes`() {
        val csv = "Name,Year,Notes\n\"The, Movie\",1999,\"He said \"\"wow\"\"\"\n"
        val table = CsvParser.parseText(csv)
        assertEquals(1, table.rows.size)
        assertEquals("The, Movie", table.rows[0]["name"])
        assertEquals("1999", table.rows[0]["year"])
        assertEquals("He said \"wow\"", table.rows[0]["notes"])
    }

    @Test
    fun `handles utf8 bom and crlf`() {
        val csv = "\uFEFFName,Year\r\nAlien,1979\r\n"
        val table = CsvParser.parseText(csv)
        assertEquals(1, table.rows.size)
        assertEquals("Alien", table.rows[0]["name"])
    }

    @Test
    fun `detects semicolon delimiter`() {
        val csv = "Name;Year\nSolaris;1972\n"
        val table = CsvParser.parseText(csv)
        assertEquals("Solaris", table.rows[0]["name"])
        assertEquals("1972", table.rows[0]["year"])
    }

    @Test
    fun `skips fully blank trailing records`() {
        val csv = "Name,Year\nAlien,1979\n\n"
        val table = CsvParser.parseText(csv)
        assertEquals(1, table.rows.size)
    }

    @Test
    fun `rejects misaligned rows`() {
        val csv = "Name,Year\nAlien\n"
        try {
            CsvParser.parseText(csv)
            throw AssertionError("Expected CsvParseException")
        } catch (e: CsvParser.CsvParseException) {
            assertTrue(e.message!!.contains("fields"))
        }
    }
}
