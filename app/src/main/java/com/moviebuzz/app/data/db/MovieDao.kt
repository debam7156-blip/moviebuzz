package com.moviebuzz.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(movie: MovieEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(movie: MovieEntity): Long

    @Update
    suspend fun update(movie: MovieEntity)

    @Query("SELECT * FROM movies ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun byId(id: Long): MovieEntity?

    @Query("SELECT * FROM movies WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun byTmdbId(tmdbId: Long): MovieEntity?

    @Query("SELECT * FROM movies WHERE imdbId = :imdbId LIMIT 1")
    suspend fun byImdbId(imdbId: String): MovieEntity?

    @Query("SELECT * FROM movies WHERE LOWER(title) = LOWER(:title) AND year = :year LIMIT 1")
    suspend fun byTitleYear(title: String, year: Int): MovieEntity?

    @Query(
        "SELECT * FROM movies WHERE LOWER(title) = LOWER(:title) ORDER BY ABS(COALESCE(year,0) - :year) ASC LIMIT 1"
    )
    suspend fun byTitleClosestYear(title: String, year: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE watched = 1 ORDER BY updatedAt DESC")
    fun observeWatched(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE watchlist = 1 ORDER BY updatedAt DESC")
    fun observeWatchlist(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY title ASC LIMIT 100")
    suspend fun search(query: String): List<MovieEntity>

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun count(): Int

    @Query("DELETE FROM movies")
    suspend fun deleteAll()
}
