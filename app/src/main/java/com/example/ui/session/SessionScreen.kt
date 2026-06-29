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
                val customCategories by viewModel.customCategories.collectAsState()
                val recentTaskNames by viewModel.recentTaskNames.collectAsState()
                StartSessionContent(
                    onStart = { taskName, category ->
                        viewModel.startSession(taskName, category)
                    },
                    customCategories = customCategories,
                    recentTaskNames = recentTaskNames,
                    onAddCustomCategory = { viewModel.addCustomCategory(it) }
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionContent(
    onStart: (String, String) -> Unit,
    customCategories: List<String>,
    recentTaskNames: List<String>,
    onAddCustomCategory: (String) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var taskDropdownExpanded by remember { mutableStateOf(false) }
    
    val defaultCategories = listOf(
        "Coding", "Study", "Video Editing", "Thumbnail Design", "Script Writing", 
        "Research", "Client Work", "Freelancing", "Meeting", "College", "Reading", 
        "Writing", "Design", "Gaming", "Exercise", "Business", "Personal", "Other"
    )
    val allCategories = (defaultCategories + customCategories).distinct()
    
    var selectedCategory by remember { mutableStateOf(allCategories.first()) }
    var expanded by remember { mutableStateOf(false) }
    
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = taskDropdownExpanded,
            onExpandedChange = { taskDropdownExpanded = !taskDropdownExpanded }
        ) {
            OutlinedTextField(
                value = taskName,
                onValueChange = { 
                    taskName = it
                    taskDropdownExpanded = true
                },
                label = { Text("What are you working on?") },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            val filteredTasks = recentTaskNames.filter { it.contains(taskName, ignoreCase = true) }
            if (filteredTasks.isNotEmpty() && taskDropdownExpanded) {
                ExposedDropdownMenu(
                    expanded = taskDropdownExpanded,
                    onDismissRequest = { taskDropdownExpanded = false }
                ) {
                    filteredTasks.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                taskName = suggestion
                                taskDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                allCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            expanded = false
                        }
                    )
                }
            }
        }
        
        TextButton(onClick = { showNewCategoryDialog = true }) {
            Text("+ Create New Category")
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
    
    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { 
                    if (newCategoryName.isNotBlank()) {
                        onAddCustomCategory(newCategoryName)
                        selectedCategory = newCategoryName
                    }
                    showNewCategoryDialog = false
                    newCategoryName = ""
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
