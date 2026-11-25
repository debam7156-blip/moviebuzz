package com.moviebuzz.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "movie_tags",
    primaryKeys = ["movieId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = MovieEntity::class, parentColumns = ["id"], childColumns = ["movieId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["movieId"]), Index(value = ["tagId"])]
)
data class MovieTagEntity(
    val movieId: Long,
    val tagId: Long
)
