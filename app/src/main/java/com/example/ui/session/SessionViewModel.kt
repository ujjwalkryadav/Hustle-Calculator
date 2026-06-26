package com.example.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.SessionBreak
import com.example.data.entity.WorkSession
import com.example.data.repository.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = SessionRepository(database.workSessionDao(), database.sessionBreakDao())

    val activeSession: StateFlow<WorkSession?> = repository.activeSession.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _elapsedTimeMillis = MutableStateFlow(0L)
    val elapsedTimeMillis: StateFlow<Long> = _elapsedTimeMillis.asStateFlow()

    init {
        // Simple timer loop
        viewModelScope.launch {
            while(true) {
                val session = repository.activeSession.firstOrNull()
                if (session != null) {
                    if (session.state == "RUNNING") {
                        // Calculate elapsed time excluding breaks
                        val breaks = repository.getBreaksForSession(session.id).firstOrNull() ?: emptyList()
                        var totalBreakTime = 0L
                        for (b in breaks) {
                            val end = b.endTime ?: System.currentTimeMillis()
                            totalBreakTime += (end - b.startTime)
                        }
                        
                        val totalElapsed = System.currentTimeMillis() - session.startTime - totalBreakTime
                        _elapsedTimeMillis.value = totalElapsed
                    }
                } else {
                    _elapsedTimeMillis.value = 0L
                }
                delay(1000)
            }
        }
    }

    fun startSession(taskName: String, category: String) {
        viewModelScope.launch {
            repository.startSession(taskName, category)
            startService()
        }
    }

    fun pauseSession(reason: String) {
        viewModelScope.launch {
            repository.pauseSession(reason)
        }
    }

    fun resumeSession() {
        viewModelScope.launch {
            repository.resumeSession()
        }
    }

    fun stopSession(notes: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.stopSession(notes)
            stopService()
            onComplete()
        }
    }

    private fun startService() {
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, com.example.service.TimerService::class.java).apply {
            action = com.example.service.TimerService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService() {
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, com.example.service.TimerService::class.java).apply {
            action = com.example.service.TimerService.ACTION_STOP
        }
        context.startService(intent)
    }
}
