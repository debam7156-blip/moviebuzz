package com.moviebuzz.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_events",
    foreignKeys = [
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["movieId"]), Index(value = ["watchedOn"], unique = false)]
)
data class WatchEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val movieId: Long,
    /** ISO date (yyyy-MM-dd) of the viewing session. */
    val watchedOn: String,
    /** Personal rating recorded at the time of this viewing; may be null for unrated sessions. */
    val ratingAtWatch: Double? = null,
    /** Optional per-viewing note (e.g. "watched with friends"). */
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
