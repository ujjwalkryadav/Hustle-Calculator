package com.example.ui.calendar

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.entity.WorkSession
import com.example.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = SessionRepository(database.workSessionDao(), database.sessionBreakDao())

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth
    
    private val _sessions = MutableStateFlow<List<WorkSession>>(emptyList())
    val sessions: StateFlow<List<WorkSession>> = _sessions

    init {
        viewModelScope.launch {
            repository.allSessions.collectLatest {
                _sessions.value = it
            }
        }
    }

    fun previousMonth() {
        _currentMonth.value = (_currentMonth.value.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    }

    fun nextMonth() {
        _currentMonth.value = (_currentMonth.value.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onDayClick: (Long) -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val currentMonth by viewModel.currentMonth.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productivity Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            MonthHeader(
                currentMonth = currentMonth,
                onPrevious = { viewModel.previousMonth() },
                onNext = { viewModel.nextMonth() }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DaysOfWeekHeader()
            Spacer(modifier = Modifier.height(8.dp))
            CalendarGrid(currentMonth = currentMonth, sessions = viewModel.sessions.collectAsState().value, onDayClick = onDayClick)
        }
    }
}

@Composable
fun MonthHeader(currentMonth: Calendar, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrevious) { Text("<") }
        val monthName = currentMonth.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        val year = currentMonth.get(Calendar.YEAR)
        Text("$monthName $year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onNext) { Text(">") }
    }
}

@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach {
            Text(it, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CalendarGrid(currentMonth: Calendar, sessions: List<WorkSession>, onDayClick: (Long) -> Unit) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sunday = 0)
    
    val days = mutableListOf<Int?>()
    for (i in 0 until startDayOfWeek) days.add(null)
    for (i in 1..daysInMonth) days.add(i)

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { day ->
            if (day == null) {
                Box(modifier = Modifier.size(40.dp))
            } else {
                val cal = (firstDayOfMonth.clone() as Calendar).apply { 
                    set(Calendar.DAY_OF_MONTH, day) 
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis
                val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
                
                val daySessions = sessions.filter { it.startTime in startOfDay..endOfDay && it.state == "COMPLETED" }
                val totalDuration = daySessions.sumOf { it.activeWorkMillis }
                val hours = totalDuration.toFloat() / (1000 * 60 * 60)
                
                val color = when {
                    daySessions.isEmpty() -> Color.Transparent
                    hours == 0f -> Color(0xFF7F1D1D) // Dark Red
                    hours < 1f -> Color(0xFFEF4444) // Red
                    hours < 2f -> Color(0xFFF97316) // Orange
                    hours < 4f -> Color(0xFFEAB308) // Yellow
                    hours < 6f -> Color(0xFF86EFAC) // Light Green
                    hours < 8f -> Color(0xFF22C55E) // Green
                    hours < 10f -> Color(0xFF16A34A) // Bright Green
                    hours < 12f -> Color(0xFF10B981) // Emerald
                    else -> Color(0xFF047857) // Dark Emerald
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onDayClick(startOfDay) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.toString(),
                        color = if (color == Color.Transparent) MaterialTheme.colorScheme.onBackground else Color.Black
                    )
                }
            }
        }
    }
}
