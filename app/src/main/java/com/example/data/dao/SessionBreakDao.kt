package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.SessionBreak
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionBreakDao {
    @Query("SELECT * FROM session_breaks ORDER BY startTime ASC")
    fun getAllBreaks(): Flow<List<SessionBreak>>

    @Query("SELECT * FROM session_breaks WHERE sessionId = :sessionId ORDER BY startTime ASC")
    fun getBreaksForSession(sessionId: Long): Flow<List<SessionBreak>>

    @Query("SELECT * FROM session_breaks WHERE sessionId = :sessionId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveBreakSync(sessionId: Long): SessionBreak?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sessionBreak: SessionBreak): Long

    @Update
    suspend fun update(sessionBreak: SessionBreak)
}
