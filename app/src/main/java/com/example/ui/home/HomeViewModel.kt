package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.repository.SessionRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimelineEvent(
    val timeFormatted: String,
    val title: String,
    val durationFormatted: String,
    val isBreak: Boolean,
    val timestamp: Long
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = SessionRepository(database.workSessionDao(), database.sessionBreakDao())
    private val settingsRepository = com.example.data.repository.SettingsRepository(application)

    val userName = settingsRepository.userName.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val allSessions = repository.allSessions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val allBreaks = repository.allBreaks

    // Calculate today's duration based on completed sessions
    val todayDurationMillis = repository.allSessions.map { sessions ->
        val todayStart = getStartOfToday()
        sessions.filter { it.startTime >= todayStart && it.state == "COMPLETED" }
            .sumOf { it.activeWorkMillis }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0L
    )
    
    val weeklyDurationMillis = repository.allSessions.map { sessions ->
        val weekStart = getStartOfWeek()
        sessions.filter { it.startTime >= weekStart && it.state == "COMPLETED" }
            .sumOf { it.activeWorkMillis }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0L
    )

    val last7DaysData = repository.allSessions.map { sessions ->
        val data = mutableListOf<Float>()
        for (i in 6 downTo 0) {
            val startOfDay = getStartOfToday() - (i * 24 * 60 * 60 * 1000L)
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L - 1
            val daySessions = sessions.filter { it.startTime in startOfDay..endOfDay && it.state == "COMPLETED" }
            val totalDuration = daySessions.sumOf { it.activeWorkMillis }
            data.add(totalDuration.toFloat() / (1000 * 60 * 60))
        }
        data
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val timelineEvents = combine(allSessions, allBreaks) { sessions, breaks ->
        val todayStart = getStartOfToday()
        val todaySessions = sessions.filter { it.startTime >= todayStart && it.state == "COMPLETED" }
        val todayBreaks = breaks.filter { it.startTime >= todayStart && it.endTime != null }
        
        val events = mutableListOf<TimelineEvent>()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        for (session in todaySessions) {
            events.add(
                TimelineEvent(
                    timeFormatted = timeFormat.format(Date(session.startTime)),
                    title = session.category,
                    durationFormatted = formatDuration(session.activeWorkMillis),
                    isBreak = false,
                    timestamp = session.startTime
                )
            )
        }
        
        for (b in todayBreaks) {
            val end = b.endTime ?: b.startTime
            val durationMs = end - b.startTime
            events.add(
                TimelineEvent(
                    timeFormatted = timeFormat.format(Date(b.startTime)),
                    title = b.reason.ifEmpty { "Break" },
                    durationFormatted = formatDuration(durationMs),
                    isBreak = true,
                    timestamp = b.startTime
                )
            )
        }
        
        events.sortedBy { it.timestamp }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private fun formatDuration(millis: Long): String {
        val hours = (millis / (1000 * 60 * 60))
        val minutes = ((millis / (1000 * 60)) % 60)
        val seconds = ((millis / 1000) % 60)
        return if (hours > 0) "${hours}h ${minutes}m ${seconds}s" else if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    private fun getStartOfToday(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getStartOfWeek(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
