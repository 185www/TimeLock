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
    private var overlay: OverlayController? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlay = OverlayController(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INTERCEPT -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_STICKY
                if (!LockState.canIntercept(pkg)) return START_STICKY
                showIntercept(pkg)
            }
            ACTION_COUNTDOWN -> {
                val secs = intent.getLongExtra(EXTRA_SECONDS, 0)
                if (secs > 0) startCountdown(secs)
            }
            ACTION_STOP -> {
                stopEverything()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timer?.cancel()
        overlay?.dismiss()
        super.onDestroy()
    }

    private fun showIntercept(packageName: String) {
        LockState.lockedPackage = packageName
        LockState.isIntercepting = true

        try {
            val notification = buildNotification("准备拦截...")
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}

        overlay?.showInterceptOverlay { minutes ->
            val seconds = minutes * 60L
            LockState.isIntercepting = false
            LockState.isTimerRunning = true
            LockState.isLocked = true
            LockState.remainingSeconds = seconds

            val countdownIntent = Intent(this, CountdownService::class.java).apply {
                action = ACTION_COUNTDOWN
                putExtra(EXTRA_SECONDS, seconds)
            }
            startService(countdownIntent)
        }
    }

    private fun startCountdown(seconds: Long) {
        timer?.cancel()
        LockState.remainingSeconds = seconds
        LockState.isTimerRunning = true

        try {
            val notification = buildNotification(formatTime(seconds))
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}

        timer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = millisUntilFinished / 1000
                LockState.remainingSeconds = remaining
                try {
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification(formatTime(remaining)))
                } catch (_: Exception) {}
            }

            override fun onFinish() {
                LockState.isTimerRunning = false
                LockState.remainingSeconds = 0L

                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (_: Exception) {}

                overlay?.showTimeUpOverlay {
                    LockState.cooldownUntil = System.currentTimeMillis() + 5000
                    LockState.isLocked = false
                    LockState.lockedPackage = ""
                }
            }
        }.start()
    }

    private fun stopEverything() {
        timer?.cancel()
        overlay?.dismiss()
        LockState.isTimerRunning = false
        LockState.isIntercepting = false
        LockState.isLocked = false
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "时间锁", NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID)
        else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("时间锁")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }

    private fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("剩余 %d分%02d秒", m, s)
    }

    companion object {
        const val ACTION_INTERCEPT = "com.timelock.action.INTERCEPT"
        const val ACTION_COUNTDOWN = "com.timelock.action.COUNTDOWN"
        const val ACTION_STOP = "com.timelock.action.STOP"
        const val EXTRA_PACKAGE = "package_name"
        const val EXTRA_SECONDS = "duration_seconds"
        private const val CHANNEL_ID = "tracker_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
