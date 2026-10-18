package com.example.photobooth.data

import kotlinx.coroutines.flow.Flow

class PhotoBoothRepository(private val sessionDao: SessionDao) {
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    suspend fun insertSession(session: SessionEntity): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun getCount(): Int {
        return sessionDao.getTotalSessionsCount()
    }

    suspend fun deleteSession(id: Long) {
        sessionDao.deleteSession(id)
    }

    suspend fun clearAllSessions() {
        sessionDao.clearAllSessions()
    }
}
