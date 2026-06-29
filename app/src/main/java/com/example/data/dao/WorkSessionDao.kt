package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.WorkSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkSessionDao {
    @Query("SELECT * FROM work_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkSession>>

    @Query("SELECT * FROM work_sessions WHERE state = 'RUNNING' OR state = 'PAUSED' LIMIT 1")
    fun getActiveSession(): Flow<WorkSession?>

    @Query("SELECT * FROM work_sessions WHERE state = 'RUNNING' OR state = 'PAUSED' LIMIT 1")
    suspend fun getActiveSessionSync(): WorkSession?

    @Query("SELECT DISTINCT taskName FROM work_sessions WHERE taskName != '' ORDER BY startTime DESC LIMIT 20")
    fun getRecentTaskNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkSession): Long

    @Update
    suspend fun update(session: WorkSession)

    @androidx.room.Delete
    suspend fun delete(session: WorkSession)
}
