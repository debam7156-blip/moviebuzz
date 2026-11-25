package com.moviebuzz.app.data.import

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Safe ZIP extractor for Letterboxd-style export archives.
 *
 * Security rules:
 * - Entry names are validated: no absolute paths, no ".." traversal, no drive letters
 * - Total uncompressed size is capped (default 200 MB) to avoid zip-bomb attacks
 * - Only .csv entries are returned; other entries are skipped
 */
object ZipExtractor {

    class ZipSecurityException(message: String) : Exception(message)

    data class ExtractedCsv(
        val entryName: String,
        val bytes: ByteArray
    )

    fun extractCsvEntries(input: InputStream, maxTotalBytes: Long = 200_000_000): List<ExtractedCsv> {
        val out = mutableListOf<ExtractedCsv>()
        var totalBytes = 0L
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                try {
                    validateEntryName(name)
                } catch (e: ZipSecurityException) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }
                if (name.lowercase().endsWith(".csv")) {
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var read: Int
                    while (zip.read(chunk).also { read = it } > 0) {
                        totalBytes += read
                        if (totalBytes > maxTotalBytes) {
                            throw ZipSecurityException("Archive too large (exceeds ${maxTotalBytes / 1_000_000} MB limit)")
                        }
                        buffer.write(chunk, 0, read)
                    }
                    out.add(ExtractedCsv(name, buffer.toByteArray()))
                } zip.closeEntry()
                entry = zip.nextEntry
                }
            return out
        }
    }

    private fun validateEntryName(name: String) {
        if (name.contains("..")) throw ZipSecurityException("Blocked path traversal: $name")
        if (name.startsWith("/")) throw ZipSecurityException("Blocked absolute path: $name")
        if (Regex("^[a-zA-Z]:").containsMatchIn(name)) throw ZipSecurityException("Blocked drive path: $name")
    }
}
