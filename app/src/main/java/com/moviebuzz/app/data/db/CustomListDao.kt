package com.moviebuzz.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
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

    @Query("SELECT * FROM custom_lists WHERE id = :id")
    fun observeListById(id: Long): Flow<CustomListEntity?>

    @Query("SELECT * FROM custom_lists WHERE name = :name LIMIT 1")
    suspend fun listByName(name: String): CustomListEntity?

    /** All collections with their movie counts, for the Collections screen. */
    @Query(
        """
        SELECT cl.*, (SELECT COUNT(*) FROM custom_list_entries e WHERE e.listId = cl.id) AS entryCount
        FROM custom_lists cl
        ORDER BY cl.name COLLATE NOCASE ASC
        """
    )
    fun observeListsWithCount(): Flow<List<CustomListWithCount>>

    @Query("UPDATE custom_lists SET name = :name, description = :description WHERE id = :id")
    suspend fun updateList(id: Long, name: String, description: String?)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: CustomListEntryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<CustomListEntryEntity>)

    @Query("SELECT movieId FROM custom_list_entries WHERE listId = :listId")
    suspend fun movieIdsForList(listId: Long): List<Long>

    @Query("SELECT movieId FROM custom_list_entries WHERE listId = :listId")
    fun observeMovieIdsForList(listId: Long): Flow<List<Long>>

    @Query("SELECT listId FROM custom_list_entries WHERE movieId = :movieId")
    fun observeListIdsForMovie(movieId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM custom_list_entries WHERE listId = :listId AND movieId = :movieId")
    suspend fun entryCount(listId: Long, movieId: Long): Int

    @Query("DELETE FROM custom_list_entries WHERE listId = :listId AND movieId = :movieId")
    suspend fun deleteEntry(listId: Long, movieId: Long)

    /** Movies inside one collection, newest first. */
    @Query(
        """
        SELECT m.* FROM movies m
        INNER JOIN custom_list_entries e ON e.movieId = m.id
        WHERE e.listId = :listId
        ORDER BY e.addedAt DESC
        """
    )
    fun observeMoviesInList(listId: Long): Flow<List<MovieEntity>>

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

    /** Projection POJO for observeListsWithCount(). */
    data class CustomListWithCount(
        @Embedded val list: CustomListEntity,
        val entryCount: Int
    )

    /** Projection POJO for snapshotEntries(). */
    data class ListEntrySnapshot(val listName: String, val movieTmdbId: Long?)
}
