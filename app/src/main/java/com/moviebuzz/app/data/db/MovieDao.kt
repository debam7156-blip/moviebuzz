package com.moviebuzz.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(movie: MovieEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(movies: List<MovieEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(movie: MovieEntity): Long

    @Update
    suspend fun update(movie: MovieEntity)

    @Query("SELECT * FROM movies ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MovieEntity>>

    /** The user's personal library: only movies with personal data attached. */
    @Query(
        """
        SELECT * FROM movies
        WHERE watched = 1 OR watchlist = 1 OR isFavorite = 1
           OR personalRating IS NOT NULL OR review IS NOT NULL
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun observePersonal(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun byId(id: Long): MovieEntity?

    @Query("SELECT * FROM movies WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun byTmdbId(tmdbId: Long): MovieEntity?

    @Query("SELECT * FROM movies WHERE tmdbId IN (:tmdbIds)")
    suspend fun byTmdbIds(tmdbIds: List<Long>): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE imdbId = :imdbId LIMIT 1")
    suspend fun byImdbId(imdbId: String): MovieEntity?

    @Query("SELECT * FROM movies WHERE LOWER(title) = LOWER(:title) AND year = :year LIMIT 1")
    suspend fun byTitleYear(title: String, year: Int): MovieEntity?

    /** Seed for personalized recommendations: best-rated watched movie with a TMDB id. */
    @Query(
        """
        SELECT * FROM movies
        WHERE watched = 1 AND tmdbId IS NOT NULL
        ORDER BY COALESCE(personalRating, 0) DESC, updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun bestWatchedWithTmdbId(): MovieEntity?

    @Query("SELECT * FROM movies WHERE watched = 1 ORDER BY updatedAt DESC")
    fun observeWatched(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE watchlist = 1 ORDER BY updatedAt DESC")
    fun observeWatchlist(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<MovieEntity>>

    @Query(
        "SELECT * FROM movies WHERE title LIKE '%' || :query || '%' COLLATE NOCASE " +
            "ORDER BY CASE WHEN watched = 1 THEN 0 ELSE 1 END, title COLLATE NOCASE LIMIT 200"
    )
    suspend fun search(query: String): List<MovieEntity>

    @Query("UPDATE movies SET watched = 1, updatedAt = :now WHERE id = :id")
    suspend fun markWatched(id: Long, now: Long)

    @Query("UPDATE movies SET watched = :watched, updatedAt = :now WHERE id = :id")
    suspend fun setWatched(id: Long, watched: Boolean, now: Long)

    @Query("UPDATE movies SET personalRating = :rating, ratingScale = :scale, updatedAt = :now WHERE id = :id")
    suspend fun setRating(id: Long, rating: Double?, scale: Int, now: Long)

    @Query("UPDATE movies SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long)

    @Query("UPDATE movies SET watchlist = :watchlist, updatedAt = :now WHERE id = :id")
    suspend fun setWatchlist(id: Long, watchlist: Boolean, now: Long)

    @Query("UPDATE movies SET review = :review, updatedAt = :now WHERE id = :id")
    suspend fun setReview(id: Long, review: String?, now: Long)

    @Query("UPDATE movies SET notes = :notes, updatedAt = :now WHERE id = :id")
    suspend fun setNotes(id: Long, notes: String?, now: Long)

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun count(): Int

    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM movies")
    suspend fun deleteAll()
}
