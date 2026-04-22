package dev.capsule.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ImageEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun sessionDao(): SessionDao
}