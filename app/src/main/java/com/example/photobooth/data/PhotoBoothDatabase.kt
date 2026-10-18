package com.example.photobooth.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class PhotoBoothDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: PhotoBoothDatabase? = null

        fun getDatabase(context: Context): PhotoBoothDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PhotoBoothDatabase::class.java,
                    "photobooth_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
