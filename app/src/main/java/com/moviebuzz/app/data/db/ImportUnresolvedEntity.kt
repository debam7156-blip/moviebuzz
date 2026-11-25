package com.moviebuzz.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rows captured from an import that could not be automatically matched to a movie.
 * The manual resolution screen shows these to the user, who can search TMDB,
 * pick the right match, skip the record, or edit the raw fields.
 */
@Entity(tableName = "import_unresolved")
data class ImportUnresolvedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val fileLabel: String,
    val rowNumber: Int,
    /** Raw CSV row as key=value pairs, serialized as JSON for display/editing. */
    val rawJson: String,
    val title: String,
    val year: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
