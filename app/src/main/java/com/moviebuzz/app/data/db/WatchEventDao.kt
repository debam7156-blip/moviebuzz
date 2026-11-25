package com.moviebuzz.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchEventDao {

    @Insert
    suspend fun insert(event: WatchEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<WatchEventEntity>): List<Long>

    @Query("SELECT * FROM watch_events WHERE movieId = :movieId ORDER BY watchedOn ASC")
    suspend fun forMovie(movieId: Long): List<WatchEventEntity>

    @Query("SELECT * FROM watch_events WHERE movieId = :movieId ORDER BY watchedOn ASC")
    fun observeForMovie(movieId: Long): Flow<List<WatchEventEntity>>

    @Query("SELECT * FROM watch_events ORDER BY watchedOn ASC")
    fun observeAll(): Flow<List<WatchEventEntity>>

    @Query("SELECT COUNT(*) FROM watch_events")
    suspend fun count(): Int

    @Query("DELETE FROM watch_events WHERE movieId = :movieId")
    suspend fun deleteForMovie(movieId: Long)

    @Query("DELETE FROM watch_events")
    suspend fun deleteAll()
}
