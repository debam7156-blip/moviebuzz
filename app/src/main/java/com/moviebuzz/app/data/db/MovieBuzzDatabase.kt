package com.moviebuzz.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
@TypeConverters(Converters::class)
abstract class MovieBuzzDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun watchEventDao(): WatchEventDao
    abstract fun customListDao(): CustomListDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var instance: MovieBuzzDatabase? = null

        fun get(context: Context): MovieBuzzDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MovieBuzzDatabase::class.java,
                    "moviebuzz.db"
                )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
    }
}
