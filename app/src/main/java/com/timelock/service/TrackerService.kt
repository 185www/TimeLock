package com.timelock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.timelock.R
import com.timelock.data.AppRepository
import com.timelock.data.LockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrackerService : Service() {

    companion object {
        const val ACTION_INTERCEPT = "com.timelock.ACTION_INTERCEPT"
        const val EXTRA_PACKAGE = "com.timelock.EXTRA_PACKAGE"
        private const val CHANNEL_ID = "timelock_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var overlay: OverlayController
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var countdownJob: Job? = null
    private var lockedPackage: String = ""

    override fun onCreate() {
        super.onCreate()
        overlay = OverlayController(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        when (intent?.action) {
            ACTION_INTERCEPT -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
                if (pkg.isNotEmpty()) {
                    lockedPackage = pkg
                    handleIntercept(pkg)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun handleIntercept(packageName: String) {
        if (!overlay.haveOverlayPermission()) {
            Log.w("TimeLock", "No overlay permission, cannot show intercept UI")
            goHome()
            finishSession()
            return
        }

        val appName = AppRepository.getAppName(this, packageName)
        overlay.showIntercept(appName) { minutes ->
            overlay.hideIntercept()
            startCountdown(minutes * 60L)
        }
    }

    private fun startCountdown(seconds: Long) {
        LockState.isIntercepting = false
        LockState.isTimerRunning = true
        LockState.remainingSeconds = seconds

        overlay.showCountdown { endSession(forceHome = true) }
        overlay.updateCountdown(seconds)

        countdownJob = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                LockState.remainingSeconds = remaining
                overlay.updateCountdown(remaining)
            }
            showTimeUp()
        }
    }

    private fun showTimeUp() {
        LockState.isTimerRunning = false
        overlay.hideCountdown()
        val appName = AppRepository.getAppName(this, lockedPackage)
        overlay.showTimeUp(appName) {
            goHome()
            finishSession()
        }
    }

    private fun endSession(forceHome: Boolean) {
        countdownJob?.cancel()
        overlay.hideCountdown()
        LockState.isTimerRunning = false
        if (forceHome) goHome()
        finishSession()
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(home)
        } catch (_: Exception) {
        }
    }

    private fun finishSession() {
        LockState.cooldownUntil = System.currentTimeMillis() + 5000
        LockState.reset()
        overlay.removeAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "时间锁",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "时间锁正在保护你"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("时间锁")
            .setContentText("正在运行中")
            .setSmallIcon(R.drawable.ic_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        countdownJob?.cancel()
        overlay.removeAll()
    }
}
