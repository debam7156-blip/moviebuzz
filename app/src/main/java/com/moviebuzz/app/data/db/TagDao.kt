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

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun tagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMovieTag(entry: MovieTagEntity)

    @Query("SELECT t.name FROM tags t INNER JOIN movie_tags mt ON t.id = mt.tagId WHERE mt.movieId = :movieId")
    suspend fun tagNamesForMovie(movieId: Long): List<String>

    @Query("SELECT t.name FROM tags t INNER JOIN movie_tags mt ON t.id = mt.tagId WHERE mt.movieId = :movieId")
    fun observeTagNamesForMovie(movieId: Long): Flow<List<String>>

    @Query("DELETE FROM movie_tags WHERE movieId = :movieId")
    suspend fun clearMovieTags(movieId: Long)

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM movie_tags")
    suspend fun deleteAllMovieTags()
}
