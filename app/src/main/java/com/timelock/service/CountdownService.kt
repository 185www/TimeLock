package com.timelock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import com.timelock.data.LockState
import com.timelock.ui.screens.TimeUpActivity

class CountdownService : Service() {

    private var timer: CountDownTimer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getLongExtra("duration_seconds", 0L) ?: 0L
        if (seconds <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        LockState.remainingSeconds = seconds
        LockState.isTimerRunning = true

        try {
            val notification = buildNotification(seconds)
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // If startForeground fails, still try to run
        }

        timer?.cancel()
        timer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = millisUntilFinished / 1000
                LockState.remainingSeconds = remaining
                try {
                    val notification = buildNotification(remaining)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, notification)
                } catch (_: Exception) {}
            }

            override fun onFinish() {
                LockState.isTimerRunning = false
                LockState.isLocked = true
                LockState.remainingSeconds = 0L

                val intent = Intent(this@CountdownService, TimeUpActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)

                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (_: Exception) {}
                stopSelf()
            }
        }.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Restart service if task is removed
        val restartIntent = Intent(this, CountdownService::class.java).apply {
            putExtra("duration_seconds", LockState.remainingSeconds)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "时间锁倒计时",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(seconds: Long): Notification {
        val minutes = seconds / 60
        val secs = seconds % 60
        val timeText = String.format("剩余 %d分%02d秒", minutes, secs)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("时间锁")
            .setContentText(timeText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "countdown_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
