package com.moviebuzz.app.data.tmdb

import kotlinx.serialization.Serializable

@Serializable
data class TmdbMovie(
    val id: Long,
    val title: String,
    val original_title: String? = null,
    val release_date: String? = null,
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val runtime: Int? = null,
    val vote_average: Double? = null,
    val vote_count: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val production_countries: List<TmdbCountry> = emptyList(),
    val spoken_languages: List<TmdbLanguage> = emptyList(),
    val production_companies: List<TmdbCompany> = emptyList(),
    val credits: TmdbCredits? = null,
    val external_ids: TmdbExternalIds? = null
) {
    val year: Int? get() = release_date?.takeIf { it.length >= 4 }?.take(4)?.toIntOrNull()
    val director: String? get() = credits?.crew?.firstOrNull { it.job == "Director" }?.name
    val castNames: List<String> get() = credits?.cast?.map { it.name } ?: emptyList()
}

@Serializable
data class TmdbGenre(val id: Int, val name: String)

@Serializable
data class TmdbCountry(val iso_3166_1: String, val name: String)

@Serializable
data class TmdbLanguage(val english_name: String, val name: String? = null)

@Serializable
data class TmdbCompany(val id: Long, val name: String)

@Serializable
data class TmdbExternalIds(val imdb_id: String? = null)
