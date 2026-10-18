package com.example.photobooth.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM photo_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("SELECT COUNT(*) FROM photo_sessions")
    suspend fun getTotalSessionsCount(): Int

    @Query("DELETE FROM photo_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM photo_sessions")
    suspend fun clearAllSessions()
}
