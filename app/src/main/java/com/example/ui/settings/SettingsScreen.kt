package com.example.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.entity.SessionBreak
import com.example.data.entity.WorkSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val workSessionDao = database.workSessionDao()
    private val sessionBreakDao = database.sessionBreakDao()
    private val settingsRepository = com.example.data.repository.SettingsRepository(application)
    
    val customCategories = settingsRepository.customCategories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList<String>()
    )
    
    fun removeCustomCategory(category: String) {
        viewModelScope.launch {
            settingsRepository.removeCustomCategory(category)
        }
    }

    fun exportData(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessions = workSessionDao.getAllSessions().firstOrNull() ?: emptyList()
                val breaks = sessionBreakDao.getAllBreaks().firstOrNull() ?: emptyList()
                
                val rootObj = JSONObject()
                rootObj.put("version", 1)
                
                val sessionsArray = JSONArray()
                sessions.forEach { session ->
                    val obj = JSONObject()
                    obj.put("id", session.id)
                    obj.put("taskName", session.taskName)
                    obj.put("category", session.category)
                    obj.put("startTime", session.startTime)
                    obj.put("endTime", session.endTime ?: -1)
                    obj.put("state", session.state)
                    obj.put("notes", session.notes)
                    obj.put("activeWorkMillis", session.activeWorkMillis)
                    obj.put("lastResumeTime", session.lastResumeTime)
                    sessionsArray.put(obj)
                }
                rootObj.put("sessions", sessionsArray)
                
                val breaksArray = JSONArray()
                breaks.forEach { b ->
                    val obj = JSONObject()
                    obj.put("id", b.id)
                    obj.put("sessionId", b.sessionId)
                    obj.put("startTime", b.startTime)
                    obj.put("endTime", b.endTime ?: -1)
                    obj.put("reason", b.reason)
                    breaksArray.put(obj)
                }
                rootObj.put("breaks", breaksArray)
                
                val jsonStr = rootObj.toString(4)
                
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(jsonStr.toByteArray())
                }
                android.util.Log.d("SettingsViewModel", "JSON Export Successful")
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("SettingsViewModel", "JSON Export Failed: ${e.message}")
                onResult(false)
            }
        }
    }

    fun importData(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: return@launch onResult(false)
                
                val rootObj = JSONObject(jsonStr)
                val sessionsArray = rootObj.optJSONArray("sessions")
                val breaksArray = rootObj.optJSONArray("breaks")
                
                // Clear existing
                database.clearAllTables()
                
                if (sessionsArray != null) {
                    for (i in 0 until sessionsArray.length()) {
                        val obj = sessionsArray.getJSONObject(i)
                        val endT = obj.optLong("endTime", -1)
                        val session = WorkSession(
                            id = obj.getLong("id"),
                            taskName = obj.getString("taskName"),
                            category = obj.getString("category"),
                            startTime = obj.getLong("startTime"),
                            endTime = if (endT == -1L) null else endT,
                            state = obj.getString("state"),
                            notes = obj.optString("notes", ""),
                            activeWorkMillis = obj.optLong("activeWorkMillis", 0L),
                            lastResumeTime = obj.optLong("lastResumeTime", obj.getLong("startTime"))
                        )
                        workSessionDao.insert(session)
                    }
                }
                
                if (breaksArray != null) {
                    for (i in 0 until breaksArray.length()) {
                        val obj = breaksArray.getJSONObject(i)
                        val endT = obj.optLong("endTime", -1)
                        val br = SessionBreak(
                            id = obj.getLong("id"),
                            sessionId = obj.getLong("sessionId"),
                            startTime = obj.getLong("startTime"),
                            endTime = if (endT == -1L) null else endT,
                            reason = obj.optString("reason", "")
                        )
                        sessionBreakDao.insert(br)
                    }
                }
                
                android.util.Log.d("SettingsViewModel", "JSON Import Successful")
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("SettingsViewModel", "JSON Import Failed: ${e.message}")
                onResult(false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var importError by remember { mutableStateOf<String?>(null) }
    val customCategories by viewModel.customCategories.collectAsState()
    
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportData(context, uri) { success ->
                if (!success) {
                    importError = "Export failed."
                }
            }
        }
    }
    
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importData(context, uri) { success ->
                if (!success) {
                    importError = "Import failed. Invalid file."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleLarge)
            
            Button(
                onClick = { 
                    val date = java.text.SimpleDateFormat("yyyy_MM_dd_HH_mm", java.util.Locale.getDefault()).format(java.util.Date())
                    exportLauncher.launch("HustleCalculator_Backup_$date.json") 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Backup")
            }
            
            OutlinedButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import Backup")
            }
            
            if (importError != null) {
                Text(importError!!, color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Custom Categories", style = MaterialTheme.typography.titleLarge)
            if (customCategories.isEmpty()) {
                Text("No custom categories created yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                customCategories.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(category, style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { viewModel.removeCustomCategory(category) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("App Updates", style = MaterialTheme.typography.titleLarge)
            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) { "1.0.0" }
            val versionCode = try {
                context.packageManager.getPackageInfo(context.packageName, 0).let {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        it.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        it.versionCode.toString()
                    }
                }
            } catch (e: Exception) { "1" }
            
            Text("Version: $versionName", style = MaterialTheme.typography.bodyMedium)
            
            Button(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://github.com/ujjwalkryadav/Hustle-Calculator/tags"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check for Updates")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("About", style = MaterialTheme.typography.titleLarge)
            Text("Application Name: Hustle Calculator", style = MaterialTheme.typography.bodyMedium)
            Text("Installed Version: $versionName", style = MaterialTheme.typography.bodyMedium)
            Text("Build Number: $versionCode", style = MaterialTheme.typography.bodyMedium)
            Text("Developer: Ujjwal Kumar Yadav", style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://github.com/ujjwalkryadav/Hustle-Calculator"))
                    context.startActivity(intent)
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Repository Link: github.com/ujjwalkryadav/Hustle-Calculator")
            }
        }
    }
}
