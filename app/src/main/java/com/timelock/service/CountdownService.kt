package com.timelock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import com.timelock.data.LockState

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
        LockState.isLocked = true

        try {
            val notification = buildNotification(seconds)
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}

        timer?.cancel()
        timer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = millisUntilFinished / 1000
                LockState.remainingSeconds = remaining
                try {
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification(remaining))
                } catch (_: Exception) {}
            }

            override fun onFinish() {
                LockState.isTimerRunning = false
                LockState.remainingSeconds = 0L

                LockAccessibilityService.instance?.showTimeUp()

                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (_: Exception) {}
                stopSelf()
            }
        }.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
            ).apply { setShowBadge(false) }
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
