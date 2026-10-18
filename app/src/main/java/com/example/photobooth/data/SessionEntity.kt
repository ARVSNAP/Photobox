package com.example.photobooth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val layoutType: String,
    val photoPaths: String,
    val printCount: Int = 1,
    val status: String = "COMPLETED"
)
