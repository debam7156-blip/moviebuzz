package com.moviebuzz.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movies",
    indices = [
        Index(value = ["tmdbId"], unique = true),
        Index(value = ["imdbId"], unique = false)
    ]
)
data class MovieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tmdbId: Long? = null,
    val imdbId: String? = null,
    val title: String,
    val originalTitle: String? = null,
    val year: Int? = null,
    val releaseDate: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    val status: String? = null,
    val trailerKey: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val runtimeMinutes: Int? = null,
    val director: String? = null,
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val productionCompanies: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val castCharacters: List<String> = emptyList(),
    val castProfiles: List<String> = emptyList(),
    val tmdbVoteAverage: Double? = null,
    val tmdbVoteCount: Int? = null,
    // Personal data
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
