package com.example.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onSessionEnded: () -> Unit,
    viewModel: SessionViewModel = viewModel()
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val elapsedTime by viewModel.elapsedTimeMillis.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeSession == null) "New Session" else "Active Session") },
                navigationIcon = {
                    IconButton(onClick = onSessionEnded) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activeSession == null) {
                StartSessionContent(onStart = { taskName, category ->
                    viewModel.startSession(taskName, category)
                })
            } else {
                ActiveSessionContent(
                    taskName = activeSession!!.taskName,
                    state = activeSession!!.state,
                    elapsedTime = elapsedTime,
                    onPause = { viewModel.pauseSession("Break") },
                    onResume = { viewModel.resumeSession() },
                    onStop = { notes -> viewModel.stopSession(notes, onSessionEnded) }
                )
            }
        }
    }
}

@Composable
fun StartSessionContent(onStart: (String, String) -> Unit) {
    var taskName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Coding") }
    
    val categories = listOf("Coding", "Video Editing", "Thumbnail", "YouTube Script", "AI Research", "Study", "Client Work")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        OutlinedTextField(
            value = taskName,
            onValueChange = { taskName = it },
            label = { Text("What are you working on?") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Text("Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { 
                val name = if (taskName.isNotBlank()) taskName else selectedCategory
                onStart(name, selectedCategory) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Start Session", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionContent(
    taskName: String,
    state: String,
    elapsedTime: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: (String) -> Unit
) {
    var showStopDialog by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = taskName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (state == "PAUSED") "Paused" else "Focusing...",
            style = MaterialTheme.typography.titleMedium,
            color = if (state == "PAUSED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Timer Display
        val seconds = (elapsedTime / 1000) % 60
        val minutes = (elapsedTime / (1000 * 60)) % 60
        val hours = (elapsedTime / (1000 * 60 * 60))

        Text(
            text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(64.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            if (state == "RUNNING") {
                FloatingActionButton(
                    onClick = onPause,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(Icons.Filled.Pause, "Pause")
                }
            } else {
                FloatingActionButton(
                    onClick = onResume,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.PlayArrow, "Resume")
                }
            }

            FloatingActionButton(
                onClick = { showStopDialog = true },
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Icon(Icons.Filled.Stop, "Stop", tint = MaterialTheme.colorScheme.onError)
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("End Session") },
            text = {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { 
                    showStopDialog = false
                    onStop(notes) 
                }) {
                    Text("Save & Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
