package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_sessions")
data class WorkSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val state: String = "RUNNING", // RUNNING, PAUSED, COMPLETED
    val notes: String = "",
    val category: String = "Other",
    val entryCreatedAt: Long = System.currentTimeMillis()
)
