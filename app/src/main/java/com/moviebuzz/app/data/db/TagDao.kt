package com.moviebuzz.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT name FROM tags ORDER BY name COLLATE NOCASE ASC")
    suspend fun snapshotTags(): List<String>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun tagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovieTag(entry: MovieTagEntity)

    @Query("SELECT t.name FROM tags t INNER JOIN movie_tags mt ON t.id = mt.tagId WHERE mt.movieId = :movieId")
    suspend fun tagNamesForMovie(movieId: Long): List<String>

    @Query("SELECT t.name FROM tags t INNER JOIN movie_tags mt ON t.id = mt.tagId WHERE mt.movieId = :movieId")
    fun observeTagNamesForMovie(movieId: Long): Flow<List<String>>

    /** Joined snapshot for backup export: movie TMDB id + tag name per link. */
    @Query(
        """
        SELECT m.tmdbId AS movieTmdbId, t.name AS tagName
        FROM movie_tags mt
        INNER JOIN movies m ON m.id = mt.movieId
        INNER JOIN tags t ON t.id = mt.tagId
        ORDER BY t.name ASC
        """
    )
    suspend fun snapshotMovieTags(): List<MovieTagSnapshot>

    @Query("DELETE FROM movie_tags WHERE movieId = :movieId")
    suspend fun clearMovieTags(movieId: Long)

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM movie_tags")
    suspend fun deleteAllMovieTags()

    /** Projection POJO for snapshotMovieTags(). */
    data class MovieTagSnapshot(val movieTmdbId: Long?, val tagName: String)
}
