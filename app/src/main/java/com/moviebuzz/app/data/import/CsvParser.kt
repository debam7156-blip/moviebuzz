package com.moviebuzz.app.data.import

import java.io.InputStream

/**
 * RFC 4180-style CSV parser tuned for real-world export files:
 * - Handles UTF-8 BOM
 * - Handles quoted fields containing commas, quotes (escaped as ""), and newlines
 * - Auto-detects the delimiter (comma, semicolon, tab, pipe) from the header line
 * - Returns rows as maps keyed by normalized lowercase header names
 */
object CsvParser {

    data class CsvTable(
        val headers: List<String>,
        val rows: List<Map<String, String>>
    ) {
        fun count(): Int = rows.size
    }

    class CsvParseException(message: String) : Exception(message)

    fun parse(input: InputStream, maxRows: Int = 200_000): CsvTable {
        val text = readAllText(input)
        if (text.isBlank()) throw CsvParseException("File is empty")
        return parseText(text, maxRows)
    }

    fun parseText(text: String, maxRows: Int = 200_000): CsvTable {
        val cleaned = stripBom(text)
        if (cleaned.isBlank()) throw CsvParseException("File is empty")
        val delimiter = detectDelimiter(cleaned)
        val records = parseRecords(cleaned, delimiter)
        if (records.isEmpty()) throw CsvParseException("No rows found")
        val headers = records.first().map { it.trim().lowercase() }
        if (headers.any { it.isBlank() }) throw CsvParseException("Header row contains empty column names")
        val rows = mutableListOf<Map<String, String>>()
        for (i in 1 until records.size) {
            if (rows.size >= maxRows) break
            val record = records[i]
            if (record.size != headers.size) {
                // Tolerate trailing empty lines, reject misaligned rows for the caller to decide
                if (record.all { it.isBlank() }) continue
                throw CsvParseException("Row ${i + 1} has ${record.size} fields but header has ${headers.size}")
            }
            rows.add(headers.indices.associate { headers[it] to record[it] })
        }
        return CsvTable(headers, rows)
    }

    private fun stripBom(text: String): String =
        if (text.startsWith("\uFEFF")) text.substring(1) else text

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull() ?: return ','
        val counts = mapOf(
            ',' to countUnquoted(firstLine, ','),
            ';' to countUnquoted(firstLine, ';'),
            '\t' to countUnquoted(firstLine, '\t'),
            '|' to countUnquoted(firstLine, '|')
        )
        return counts.maxByOrNull { it.value }?.key ?: ','
    }

    private fun countUnquoted(line: String, delim: Char): Int {
        var inQuotes = false
        var count = 0
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delim && !inQuotes -> count++
            }
        }
        return count
    }

    private fun parseRecords(text: String, delimiter: Char): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val field = StringBuilder()
        val fields = mutableListOf<String>()
        var i = 0
        var inQuotes = false
        var fieldStarted = false
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> {
                    when {
                        c == '"' -> {
                            if (i + 1 < text.length && text[i + 1] == '"') {
                                field.append('"'); i++
                            } else {
                                inQuotes = false
                            }
                        }
                        else -> field.append(c)
                    }
                }
                else -> when (c) commaCase@{ }
            }
            i++
        }
        return records
    }
}
