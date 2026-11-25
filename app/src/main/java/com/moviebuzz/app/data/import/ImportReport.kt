package com.moviebuzz.app.data.import

import com.moviebuzz.app.data.db.ImportUnresolvedEntity

/**
 * Result of a full import run. Every record in the source file is accounted for:
 * matched, duplicate, or unresolved. Nothing is silently dropped.
 */
data class ImportReport(
    val source: String,
    val totalRecords: Int,
    val imported: Int,
    val duplicates: Int,
    val unresolved: Int,
    val failed: Int,
    val importedLists: List<String>,
    val missingInfoNotes: List<String>,
    val unresolvedRows: List<ImportUnresolvedEntity> = emptyList()
) {
    val accounted: Boolean get() = imported + duplicates + unresolved + failed == totalRecords
}
