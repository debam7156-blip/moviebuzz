package com.moviebuzz.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MovieBuzzDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun watchEventDao(): WatchEventDao
    abstract fun customListDao(): CustomListDao
    abstract fun tagDao(): TagDao
    abstract fun importUnresolvedDao(): ImportUnresolvedDao

    companion object {
        @Volatile
        private var instance: MovieBuzzDatabase? = null

        /** v1 -> v2: detail enrichment columns (tagline, status, trailer, cast extras). */
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN tagline TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN status TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN trailerKey TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN castCharacters TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE movies ADD COLUMN castProfiles TEXT NOT NULL DEFAULT '[]'")
            }
        }

        fun get(context: Context): MovieBuzzDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MovieBuzzDatabase::class.java,
                    "moviebuzz.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
    }
}
