package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.service.TimerService
import com.example.ui.HustleApp
import com.example.ui.theme.HustleTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) {}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Restore foreground service if a session was running when app was closed
    lifecycleScope.launch {
        val database = AppDatabase.getDatabase(this@MainActivity)
        val activeSession = database.workSessionDao().getActiveSessionSync()
        if (activeSession != null) {
            val intent = Intent(this@MainActivity, TimerService::class.java).apply {
                action = TimerService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    enableEdgeToEdge()
    setContent { 
        HustleTheme { 
            HustleApp(shouldOpenSession = intent.getBooleanExtra("open_session", false)) 
        } 
    }
  }

  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      setContent { 
          HustleTheme { 
              HustleApp(shouldOpenSession = intent.getBooleanExtra("open_session", false)) 
          } 
      }
  }
}
