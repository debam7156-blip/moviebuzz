package com.moviebuzz.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "custom_lists")
data class CustomListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "custom_list_entries",
    primaryKeys = ["listId", "movieId"],
    foreignKeys = [
        ForeignKey(
            entity = CustomListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"]), Index(value = ["movieId"])]
)
data class CustomListEntryEntity(
    val listId: Long,
    val movieId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
