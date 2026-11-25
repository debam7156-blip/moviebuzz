package com.moviebuzz.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.json.Json

class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString("|||")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("|||")
}

@Database(
    entities = [
        MovieEntity::class,
        WatchEventEntity::class,
        CustomListEntity::class,
        CustomListEntryEntity::class,
        TagEntity::class,
        MovieTagEntity::class,
        ImportUnresolvedEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class MovieBuzzDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun watchEventDao(): WatchEventDao
    abstract fun customListDao(): CustomListDao
    abstract fun tagDao(): TagDao
    abstract fun importUnresolvedDao(): ImportUnresolvedDao
}
