package com.moviebuzz.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomListDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertList(list: CustomListEntity): Long

    @Query("SELECT * FROM custom_lists ORDER BY name COLLATE NOCASE ASC")
    fun observeLists(): Flow<List<CustomListEntity>>

    @Query("SELECT * FROM custom_lists ORDER BY id ASC")
    suspend fun snapshotLists(): List<CustomListEntity>

    @Query("SELECT * FROM custom_lists WHERE id = :id")
    suspend fun listById(id: Long): CustomListEntity?

    @Query("SELECT * FROM custom_lists WHERE name = :name LIMIT 1")
    suspend fun listByName(name: String): CustomListEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: CustomListEntryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<CustomListEntryEntity>)

    @Query("SELECT movieId FROM custom_list_entries WHERE listId = :listId")
    suspend fun movieIdsForList(listId: Long): List<Long>

    @Query("SELECT movieId FROM custom_list_entries WHERE listId = :listId")
    fun observeMovieIdsForList(listId: Long): Flow<List<Long>>

    /** Joined snapshot for backup export: list name + movie TMDB id per entry. */
    @Query(
        """
        SELECT cl.name AS listName, m.tmdbId AS movieTmdbId
        FROM custom_list_entries e
        INNER JOIN custom_lists cl ON cl.id = e.listId
        INNER JOIN movies m ON m.id = e.movieId
        ORDER BY cl.id ASC
        """
    )
    suspend fun snapshotEntries(): List<ListEntrySnapshot>

    @Query("DELETE FROM custom_lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Query("DELETE FROM custom_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM custom_list_entries")
    suspend fun deleteAllEntries()

    /** Projection POJO for snapshotEntries(). */
    data class ListEntrySnapshot(val listName: String, val movieTmdbId: Long?)
}
