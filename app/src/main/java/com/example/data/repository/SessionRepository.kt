package com.example.data.repository

import com.example.data.dao.SessionBreakDao
import com.example.data.dao.WorkSessionDao
import com.example.data.entity.SessionBreak
import com.example.data.entity.WorkSession
import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val workSessionDao: WorkSessionDao,
    private val sessionBreakDao: SessionBreakDao
) {
    val allSessions: Flow<List<WorkSession>> = workSessionDao.getAllSessions()
    val allBreaks: Flow<List<SessionBreak>> = sessionBreakDao.getAllBreaks()
    val activeSession: Flow<WorkSession?> = workSessionDao.getActiveSession()
    val recentTaskNames: Flow<List<String>> = workSessionDao.getRecentTaskNames()

    suspend fun startSession(taskName: String, category: String) {
        val now = System.currentTimeMillis()
        val newSession = WorkSession(
            taskName = taskName,
            category = category,
            startTime = now,
            lastResumeTime = now,
            state = "RUNNING"
        )
        workSessionDao.insert(newSession)
        android.util.Log.d("SessionRepository", "Database Save: Started Session")
    }

    suspend fun pauseSession(reason: String) {
        val session = workSessionDao.getActiveSessionSync() ?: return
        if (session.state == "RUNNING") {
            val now = System.currentTimeMillis()
            val newActiveMillis = session.activeWorkMillis + (now - session.lastResumeTime)
            // Update session state
            workSessionDao.update(session.copy(
                state = "PAUSED",
                activeWorkMillis = newActiveMillis
            ))
            android.util.Log.d("SessionRepository", "Database Save: Paused Session")
        }
    }

    suspend fun resumeSession() {
        val session = workSessionDao.getActiveSessionSync() ?: return
        if (session.state == "PAUSED") {
            // Update session state
            workSessionDao.update(session.copy(
                state = "RUNNING",
                lastResumeTime = System.currentTimeMillis()
            ))
            android.util.Log.d("SessionRepository", "Database Save: Resumed Session")
        }
    }

    suspend fun stopSession(notes: String) {
        val session = workSessionDao.getActiveSessionSync() ?: return
        
        val now = System.currentTimeMillis()
        val finalActiveMillis = if (session.state == "RUNNING") {
            session.activeWorkMillis + (now - session.lastResumeTime)
        } else {
            session.activeWorkMillis
        }

        workSessionDao.update(
            session.copy(
                state = "COMPLETED",
                endTime = now,
                notes = notes,
                activeWorkMillis = finalActiveMillis
            )
        )
        android.util.Log.d("SessionRepository", "Database Save: Stopped Session")
    }
    
    fun getBreaksForSession(sessionId: Long): Flow<List<SessionBreak>> {
        return sessionBreakDao.getBreaksForSession(sessionId)
    }
}
