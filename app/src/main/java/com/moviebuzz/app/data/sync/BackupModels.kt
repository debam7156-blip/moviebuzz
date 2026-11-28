package com.moviebuzz.app.data.sync

import com.moviebuzz.app.data.db.ImportUnresolvedEntity
import kotlinx.serialization.Serializable

/**
 * Versioned backup / cloud-sync payload. The version field lets restore logic
 * migrate older exports instead of failing. The export is a plain JSON file
 * the user owns and can move between devices freely.
 */
@Serializable
data class BackupPayload(
    val format: String = "moviebuzz-backup",
    val version: Int = 2,
    val exportedAt: Long,
    val movies: List<BackupMovie> = emptyList(),
    val watchEvents: List<BackupWatchEvent> = emptyList(),
    val lists: List<BackupList> = emptyList(),
    val listEntries: List<BackupListEntry> = emptyList(),
    val tags: List<String> = emptyList(),
    val movieTags: List<BackupMovieTag> = emptyList(),
    val importIssues: List<BackupImportIssue> = emptyList()
)

@Serializable
data class BackupMovie(
    val tmdbId: Long? = null,
    val imdbId: String? = null,
    val title: String,
    val year: Int? = null,
    val releaseDate: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val runtimeMinutes: Int? = null,
    val director: String? = null,
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val productionCompanies: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val tmdbVoteAverage: Double? = null,
    val tmdbVoteCount: Int? = null,
    val watched: Boolean = false,
    val personalRating: Double? = null,
    val ratingScale: Int = 10,
    val review: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val watchlist: Boolean = false,
    val addedAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
data class BackupWatchEvent(
    val movieTmdbId: Long?,
    val watchedOn: String,
    val ratingAtWatch: Double? = null,
    val note: String? = null,
    val createdAt: Long = 0
)

@Serializable
data class BackupList(
    val name: String,
    val description: String? = null,
    val createdAt: Long = 0
)

@Serializable
data class BackupListEntry(
    val listName: String,
    val movieTmdbId: Long?
)

@Serializable
data class BackupMovieTag(
    val movieTmdbId: Long?,
    val tagName: String
)

/** An unresolved import row, preserved in the backup so review is never lost. */
@Serializable
data class BackupImportIssue(
    val source: String,
    val fileLabel: String,
    val rowNumber: Int,
    val rawJson: String,
    val title: String,
    val year: Int? = null,
    val createdAt: Long = 0
) {
    companion object {
        fun fromEntity(e: ImportUnresolvedEntity) = BackupImportIssue(
            source = e.source,
            fileLabel = e.fileLabel,
            rowNumber = e.rowNumber,
            rawJson = e.rawJson,
            title = e.title,
            year = e.year,
            createdAt = e.createdAt
        )
    }

    fun toEntity() = ImportUnresolvedEntity(
        source = source,
        fileLabel = fileLabel,
        rowNumber = rowNumber,
        rawJson = rawJson,
        title = title,
        year = year,
        createdAt = createdAt
    )
}
