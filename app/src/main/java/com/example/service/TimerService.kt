package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TimerService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var repository: SessionRepository

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = SessionRepository(database.workSessionDao(), database.sessionBreakDao())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                android.util.Log.d("TimerService", "Timer Start action received")
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(NOTIFICATION_ID, buildNotification("Timer started", "00:00:00", "RUNNING"), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification("Timer started", "00:00:00", "RUNNING"))
                }
                android.util.Log.d("TimerService", "Foreground Service Started")
                startTimerLoop()
            }
            ACTION_PAUSE -> {
                android.util.Log.d("TimerService", "Pause action received")
                scope.launch { repository.pauseSession("Break") }
            }
            ACTION_RESUME -> {
                android.util.Log.d("TimerService", "Resume action received")
                scope.launch { repository.resumeSession() }
            }
            ACTION_STOP -> {
                android.util.Log.d("TimerService", "Timer Stop action received")
                scope.launch { repository.stopSession("") }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTimerLoop() {
        scope.launch {
            while (true) {
                val session = repository.activeSession.firstOrNull()
                if (session != null && (session.state == "RUNNING" || session.state == "PAUSED")) {
                    val totalElapsed = if (session.state == "RUNNING") {
                        session.activeWorkMillis + (System.currentTimeMillis() - session.lastResumeTime)
                    } else {
                        session.activeWorkMillis
                    }
                    
                    val hours = (totalElapsed / (1000 * 60 * 60))
                    val minutes = ((totalElapsed / (1000 * 60)) % 60)
                    val seconds = ((totalElapsed / 1000) % 60)
                    
                    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    
                    // Calculate today's total time
                    val allSessions = repository.allSessions.firstOrNull() ?: emptyList()
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val todayStart = calendar.timeInMillis
                    
                    var todayTotal = totalElapsed // Include current session
                    for (s in allSessions) {
                        if (s.startTime >= todayStart && s.state == "COMPLETED" && s.id != session.id) {
                            todayTotal += s.activeWorkMillis
                        }
                    }
                    
                    val tHours = (todayTotal / (1000 * 60 * 60))
                    val tMinutes = ((todayTotal / (1000 * 60)) % 60)
                    val tSeconds = ((todayTotal / 1000) % 60)
                    val todayTotalStr = String.format("%02dh %02dm %02ds", tHours, tMinutes, tSeconds)
                    
                    updateNotification(session.taskName, timeString, session.state, todayTotalStr)
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun buildNotification(title: String, timeString: String, state: String, todayTotalStr: String = ""): Notification {
        android.util.Log.d("TimerService", "Notification Created for state: $state")
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_session", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = PendingIntent.getService(this, 1, Intent(this, TimerService::class.java).setAction(ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val resumeIntent = PendingIntent.getService(this, 2, Intent(this, TimerService::class.java).setAction(ACTION_RESUME), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stopIntent = PendingIntent.getService(this, 3, Intent(this, TimerService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val status = if (state == "RUNNING") "🟢 Working" else "🟡 Paused"
        val contentText = "⏱ $timeString | $status"
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🏷 $title")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (todayTotalStr.isNotEmpty()) {
            builder.setSubText("📅 Today: $todayTotalStr")
        }

        if (state == "RUNNING") {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
        } else if (state == "PAUSED") {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
        }
        builder.addAction(android.R.drawable.ic_delete, "Stop", stopIntent)

        return builder.build()
    }

    private fun updateNotification(title: String, timeString: String, state: String, todayTotalStr: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, timeString, state, todayTotalStr))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1
    }
}
