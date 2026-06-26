package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.SessionBreak
import com.example.data.entity.WorkSession
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val database = AppDatabase.getDatabase(application)
    
    val isSetupComplete = settingsRepository.isSetupComplete.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )
    
    fun completeSetup(name: String = "") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                settingsRepository.setUserName(name)
            }
            settingsRepository.setSetupComplete(true)
        }
    }
    
    fun importDataAndCompleteSetup(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: return@launch onResult(false)
                
                val rootObj = JSONObject(jsonStr)
                val sessionsArray = rootObj.optJSONArray("sessions")
                val breaksArray = rootObj.optJSONArray("breaks")
                
                val workSessionDao = database.workSessionDao()
                val sessionBreakDao = database.sessionBreakDao()
                
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
                            notes = obj.optString("notes", "")
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
                
                settingsRepository.setSetupComplete(true)
                android.util.Log.d("AppViewModel", "JSON Import Successful")
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AppViewModel", "JSON Import Failed: ${e.message}")
                onResult(false)
            }
        }
    }
}
