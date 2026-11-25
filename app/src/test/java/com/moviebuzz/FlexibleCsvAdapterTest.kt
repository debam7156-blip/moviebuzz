package com.moviebuzz

import com.moviebuzz.app.data.import.ImportAdapters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlexibleCsvAdapterTest {

    @Test
    fun `parses generic csv with typical headers`() {
        val csv = "Title,Year,My Rating,Watched Date,Favorite\n" +
            "Parasite,2019,9,2020-10-01,true\n" +
            "Memento,2000,8,2021-03-04,false\n"
        val results = ImportAdapters.FlexibleCsv.fromCsv(csv.toByteArray(), "test.csv")
        val parsed = results.filterIsInstance<ImportAdapters.Parsed>()
        assertEquals(2, parsed.size)
        val parasite = parsed.first { it.record.title == "Parasite" }
        assertEquals(9.0, parasite.record.rating!!, 0.001)
        assertEquals("2020-10-01", parasite.record.watchedOn)
        assertTrue(parasite.record.isFavorite)
    }

    @Test
    fun `normalizes percent ratings to 10 scale`() {
        val csv = "Title,Rating\nOldboy,85%\n"
        val results = ImportAdapters.FlexibleCsv.fromCsv(csv.toByteArray(), "test.csv")
        val parsed = results.filterIsInstance<ImportAdapters.Parsed>().first()
        assertEquals(8.5, parsed.record.rating!!, 0.001)
    }

    @Test
    fun `normalizes slash ratings to 10 scale`() {
        val csv = "Title,Rating\nAmelie,4/5\n"
        val results = ImportAdapters.FlexibleCsv.fromCsv(csv.toByteArray(), "test.csv")
        val parsed = results.filterIsInstance<ImportAdapters.Parsed>().first()
        assertEquals(8.0, parsed.record.rating!!, 0.001)
    }

    @Test
    fun `missing title column is reported as file error`() {
        val csv = "Year,Rating\n2019,9\n"
        val results = ImportAdapters.FlexibleCsv.fromCsv(csv.toByteArray(), "test.csv")
        assertTrue(results.first() is ImportAdapters.FileError)
    }

    @Test
    fun `blank title row is reported as unparsed row`() {
        val csv = "Title,Year\n,2019\n"
        val results = ImportAdapters.FlexibleCsv.fromCsv(csv.toByteArray(), "test.csv")
        assertTrue(results.first() is ImportAdapters.UnparsedRow)
    }

    @Test
    fun `detects source from headers`() {
        val csv = "Title,Moviebase Id,Rating\nX,123,9\n"
        val results = ImportAdapters.FlexibleCsv.fromCsv(csv.toByteArray(), "test.csv")
        val parsed = results.filterIsInstance<ImportAdapters.Parsed>().first()
        assertEquals("moviebase", parsed.record.source)
    }
}
