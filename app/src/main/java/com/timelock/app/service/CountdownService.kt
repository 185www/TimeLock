package com.timelock.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.timelock.app.MainActivity
import com.timelock.app.R
import com.timelock.app.data.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Foreground service that keeps the countdown alive (and visible in the
 * notification shade) while a usage session is active. This is the "background
 * protection" piece: even if the app's UI is closed, the timer keeps running
 * and the accessibility service enforces the exit when time is up.
 */
class CountdownService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repo: SessionRepository

    override fun onCreate() {
        super.onCreate()
        repo = SessionRepository(this)
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = runBlockingSession()
        if (session == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification(session))
        scope.launch { tickLoop() }
        return START_STICKY
    }

    private fun runBlockingSession() =
        kotlinx.coroutines.runBlocking { repo.getSession() }

    private suspend fun tickLoop() {
        while (isActive) {
            val session = repo.getSession()
            if (session == null || System.currentTimeMillis() >= session.endTime) {
                // Time's up (or session gone): drop it so reopening prompts fresh,
                // then stop. The accessibility service does the actual force-close
                // when the app is in the foreground.
                if (session != null) repo.clearSession()
                stopSelf()
                return
            }
            updateNotification(session)
            delay(1000)
        }
    }

    private fun buildNotification(session: SessionRepository.Session): android.app.Notification {
        val remaining = (session.endTime - System.currentTimeMillis()).coerceAtLeast(0L)
        val label = appLabel(session.packageName)
        val text = getString(
            R.string.notification_timer_text,
            label,
            formatDuration(remaining),
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun updateNotification(session: SessionRepository.Session) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager == null || !channelExists(manager)) {
            ensureChannel()
        }
        manager?.notify(NOTIF_ID, buildNotification(session))
    }

    private fun channelExists(manager: NotificationManager): Boolean =
        manager.getNotificationChannel(CHANNEL_ID) != null

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_timer),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.channel_timer_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun appLabel(packageName: String): String = runCatching {
        val pm = packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "timer_channel"
        private const val EXTRA_PACKAGE = "extra_package"
        private const val EXTRA_END_TIME = "extra_end_time"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startIfNeeded(context: Context, packageName: String, endTime: Long) {
            if (isRunning) return
            val intent = Intent(context, CountdownService::class.java).apply {
                putExtra(EXTRA_PACKAGE, packageName)
                putExtra(EXTRA_END_TIME, endTime)
            }
            runCatching { context.startForegroundService(intent) }
        }
    }
}

internal fun formatDuration(millis: Long): String {
    val total = TimeUnit.MILLISECONDS.toSeconds(millis)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}
