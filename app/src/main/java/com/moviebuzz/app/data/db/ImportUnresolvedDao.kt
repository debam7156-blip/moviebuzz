package com.moviebuzz.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportUnresolvedDao {

    @Insert
    suspend fun insert(row: ImportUnresolvedEntity): Long

    @Insert
    suspend fun insertAll(rows: List<ImportUnresolvedEntity>)

    @Query("SELECT * FROM import_unresolved ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ImportUnresolvedEntity>>

    @Query("SELECT * FROM import_unresolved WHERE id = :id")
    suspend fun byId(id: Long): ImportUnresolvedEntity?

    /** Full snapshot for backup export - no row cap, newest first. */
    @Query("SELECT * FROM import_unresolved ORDER BY createdAt DESC")
    suspend fun snapshotAll(): List<ImportUnresolvedEntity>

    /** Identity check so restoring the same backup twice never duplicates a row. */
    @Query("SELECT COUNT(*) FROM import_unresolved WHERE fileLabel = :fileLabel AND rowNumber = :rowNumber AND source = :source AND title = :title")
    suspend fun countIdentical(fileLabel: String, rowNumber: Int, source: String, title: String): Int

    @Query("DELETE FROM import_unresolved WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM import_unresolved")
    suspend fun deleteAll()
}
