package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_breaks")
data class SessionBreak(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val reason: String = ""
)
