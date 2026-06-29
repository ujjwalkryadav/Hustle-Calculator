package com.example.ui.manual

import android.app.Application
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.example.data.AppDatabase
import com.example.data.entity.SessionBreak
import com.example.data.entity.WorkSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ManualEntryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val workSessionDao = database.workSessionDao()
    private val sessionBreakDao = database.sessionBreakDao()
    private val settingsRepository = com.example.data.repository.SettingsRepository(application)
    
    val customCategories = settingsRepository.customCategories.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        emptyList<String>()
    )

    fun saveSession(
        taskName: String,
        category: String,
        notes: String,
        durationMinutes: Int,
        breakMinutes: Int,
        breakReason: String,
        dateMillis: Long
    ) {
        viewModelScope.launch {
            val totalMillis = durationMinutes * 60 * 1000L
            val breakMillis = breakMinutes * 60 * 1000L
            
            // Assume the session starts at the given date (start of day + offset, or just use the current time if date is today)
            // For simplicity, let's just make it end at the selected date + totalMillis
            val startTime = dateMillis
            val endTime = dateMillis + totalMillis + breakMillis
            
            val session = WorkSession(
                taskName = taskName,
                category = category,
                startTime = startTime,
                endTime = endTime,
                state = "COMPLETED",
                notes = notes,
                activeWorkMillis = totalMillis,
                lastResumeTime = startTime
            )
            val sessionId = workSessionDao.insert(session)
            
            if (breakMinutes > 0) {
                val breakObj = SessionBreak(
                    sessionId = sessionId,
                    startTime = startTime + totalMillis / 2, // Put the break in the middle
                    endTime = startTime + totalMillis / 2 + breakMillis,
                    reason = breakReason
                )
                sessionBreakDao.insert(breakObj)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onBack: () -> Unit,
    viewModel: ManualEntryViewModel = viewModel()
) {
    var taskName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Work") }
    var durationStr by remember { mutableStateOf("") }
    var breakStr by remember { mutableStateOf("") }
    var breakReason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    
    val customCategories by viewModel.customCategories.collectAsState()
    val defaultCategories = listOf(
        "Coding", "Study", "Video Editing", "Thumbnail Design", "Script Writing", 
        "Research", "Client Work", "Freelancing", "Meeting", "College", "Reading", 
        "Writing", "Design", "Gaming", "Exercise", "Business", "Personal", "Other"
    )
    val allCategories = (defaultCategories + customCategories).distinct()
    var categoryExpanded by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Entry") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = dateFormat.format(selectedCalendar.time),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.weight(1f).clickable {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                selectedCalendar = (selectedCalendar.clone() as Calendar).apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                            },
                            selectedCalendar.get(Calendar.YEAR),
                            selectedCalendar.get(Calendar.MONTH),
                            selectedCalendar.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            datePicker.maxDate = System.currentTimeMillis()
                        }.show()
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                OutlinedTextField(
                    value = timeFormat.format(selectedCalendar.time),
                    onValueChange = {},
                    label = { Text("Start Time") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.weight(1f).clickable {
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                selectedCalendar = (selectedCalendar.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                }
                            },
                            selectedCalendar.get(Calendar.HOUR_OF_DAY),
                            selectedCalendar.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Task Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    allCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it },
                    label = { Text("Duration (mins)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = breakStr,
                    onValueChange = { breakStr = it },
                    label = { Text("Break (mins)") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            OutlinedTextField(
                value = breakReason,
                onValueChange = { breakReason = it },
                label = { Text("Break Reason") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    viewModel.saveSession(
                        taskName = taskName.ifEmpty { "Manual Session" },
                        category = category.ifEmpty { "Work" },
                        notes = notes,
                        durationMinutes = durationStr.toIntOrNull() ?: 60,
                        breakMinutes = breakStr.toIntOrNull() ?: 0,
                        breakReason = breakReason,
                        dateMillis = selectedCalendar.timeInMillis
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Session")
            }
        }
    }
}
