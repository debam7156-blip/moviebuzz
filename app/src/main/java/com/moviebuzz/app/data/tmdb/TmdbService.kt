package com.moviebuzz.app.data.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** TMDB endpoints. Auth key is injected at runtime by an OkHttp interceptor. */
interface TmdbService {

    @GET("movie/{movie_id}")
    suspend fun movieDetails(
        @Path("movie_id") movieId: Long,
        @Query("append_to_response") append: String = "credits,external_ids,videos,similar,recommendations"
    ): TmdbMovie

    @GET("search/movie")
    suspend fun search(
        @Query("query") query: String,
        @Query("year") year: Int? = null,
        @Query("page") page: Int = 1
    ): TmdbPage

    @GET("trending/movie/week")
    suspend fun trending(@Query("page") page: Int = 1): TmdbPage

    @GET("movie/popular")
    suspend fun popular(@Query("page") page: Int = 1): TmdbPage

    @GET("movie/upcoming")
    suspend fun upcoming(@Query("page") page: Int = 1): TmdbPage

    @GET("movie/top_rated")
    suspend fun topRated(@Query("page") page: Int = 1): TmdbPage

    @GET("movie/now_playing")
    suspend fun nowPlaying(@Query("page") page: Int = 1): TmdbPage

    @GET("movie/{movie_id}/recommendations")
    suspend fun recommendations(
        @Path("movie_id") movieId: Long,
        @Query("page") page: Int = 1
    ): TmdbPage

    @GET("movie/{movie_id}/similar")
    suspend fun similar(
        @Path("movie_id") movieId: Long,
        @Query("page") page: Int = 1
    ): TmdbPage
}
