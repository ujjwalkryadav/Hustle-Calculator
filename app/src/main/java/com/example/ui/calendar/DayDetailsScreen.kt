package com.example.ui.calendar

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.entity.WorkSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DayDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val workSessionDao = database.workSessionDao()
    private val sessionBreakDao = database.sessionBreakDao()
    
    private val _sessions = MutableStateFlow<List<WorkSession>>(emptyList())
    val sessions: StateFlow<List<WorkSession>> = _sessions
    
    fun loadSessionsForDate(startOfDay: Long, endOfDay: Long) {
        viewModelScope.launch {
            workSessionDao.getAllSessions().collectLatest { allSessions ->
                _sessions.value = allSessions.filter { 
                    it.startTime in startOfDay..endOfDay && it.state == "COMPLETED" 
                }
            }
        }
    }
    
    fun deleteSession(session: WorkSession) {
        viewModelScope.launch {
            workSessionDao.delete(session)
            // Also clean up breaks if any (could do cascaded delete, but manual is fine)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsScreen(
    dateMillis: Long,
    onBack: () -> Unit,
    viewModel: DayDetailsViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    
    LaunchedEffect(dateMillis) {
        val endOfDay = dateMillis + 24 * 60 * 60 * 1000 - 1
        viewModel.loadSessionsForDate(dateMillis, endOfDay)
    }

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = dateFormatter.format(Date(dateMillis))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dateString) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No work recorded for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val totalDuration = sessions.sumOf { (it.endTime ?: it.startTime) - it.startTime }
                val hours = (totalDuration / (1000 * 60 * 60))
                val minutes = ((totalDuration / (1000 * 60)) % 60)
                
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Work Time: ${hours}h ${minutes}m", fontWeight = FontWeight.Bold)
                            Text("Total Sessions: ${sessions.size}")
                        }
                    }
                }
                
                items(sessions) { session ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Session") },
                            text = { Text("Are you sure you want to delete this session?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteSession(session)
                                    showDeleteDialog = false
                                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(session.taskName, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text("Category: ${session.category}", style = MaterialTheme.typography.bodySmall)
                            
                            val sDuration = (session.endTime ?: session.startTime) - session.startTime
                            val sHours = (sDuration / (1000 * 60 * 60))
                            val sMins = ((sDuration / (1000 * 60)) % 60)
                            Text("Duration: ${sHours}h ${sMins}m", style = MaterialTheme.typography.bodySmall)
                            
                            if (session.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(session.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
