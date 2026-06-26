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

    suspend fun startSession(taskName: String, category: String) {
        val newSession = WorkSession(
            taskName = taskName,
            category = category,
            startTime = System.currentTimeMillis(),
            state = "RUNNING"
        )
        workSessionDao.insert(newSession)
        android.util.Log.d("SessionRepository", "Database Save: Started Session")
    }

    suspend fun pauseSession(reason: String) {
        val session = workSessionDao.getActiveSessionSync() ?: return
        if (session.state == "RUNNING") {
            // Update session state
            workSessionDao.update(session.copy(state = "PAUSED"))
            // Create break
            val newBreak = SessionBreak(
                sessionId = session.id,
                startTime = System.currentTimeMillis(),
                reason = reason
            )
            sessionBreakDao.insert(newBreak)
            android.util.Log.d("SessionRepository", "Database Save: Paused Session")
        }
    }

    suspend fun resumeSession() {
        val session = workSessionDao.getActiveSessionSync() ?: return
        if (session.state == "PAUSED") {
            // Update session state
            workSessionDao.update(session.copy(state = "RUNNING"))
            // End active break
            val activeBreak = sessionBreakDao.getActiveBreakSync(session.id)
            if (activeBreak != null) {
                sessionBreakDao.update(activeBreak.copy(endTime = System.currentTimeMillis()))
            }
            android.util.Log.d("SessionRepository", "Database Save: Resumed Session")
        }
    }

    suspend fun stopSession(notes: String) {
        val session = workSessionDao.getActiveSessionSync() ?: return
        
        // If it was paused, we need to end the break first
        if (session.state == "PAUSED") {
            val activeBreak = sessionBreakDao.getActiveBreakSync(session.id)
            if (activeBreak != null) {
                sessionBreakDao.update(activeBreak.copy(endTime = System.currentTimeMillis()))
            }
        }

        workSessionDao.update(
            session.copy(
                state = "COMPLETED",
                endTime = System.currentTimeMillis(),
                notes = notes
            )
        )
        android.util.Log.d("SessionRepository", "Database Save: Stopped Session")
    }
    
    fun getBreaksForSession(sessionId: Long): Flow<List<SessionBreak>> {
        return sessionBreakDao.getBreaksForSession(sessionId)
    }
}
