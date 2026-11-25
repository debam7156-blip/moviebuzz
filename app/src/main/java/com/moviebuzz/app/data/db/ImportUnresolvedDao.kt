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

    @Query("DELETE FROM import_unresolved WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM import_unresolved")
    suspend fun deleteAll()
}
