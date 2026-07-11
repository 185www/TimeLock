package com.timelock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import com.timelock.data.AppRepository
import com.timelock.data.LockState

class LockAccessibilityService : AccessibilityService() {

    private lateinit var overlay: OverlayController

    override fun onServiceConnected() {
        instance = this
        overlay = OverlayController(this)

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName.toString()
        if (!LockState.canIntercept(packageName)) return

        if (AppRepository.isMonitored(packageName)) {
            LockState.lockedPackage = packageName
            LockState.isIntercepting = true

            overlay.showIntercept { minutes ->
                val seconds = minutes * 60L
                LockState.isIntercepting = false
                LockState.isLocked = true
                LockState.isTimerRunning = true
                startCountdown(seconds)
            }
        }
    }

    private fun startCountdown(seconds: Long) {
        val intent = android.content.Intent(this, CountdownService::class.java).apply {
            putExtra("duration_seconds", seconds)
            putExtra("from_overlay", true)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        overlay.dismiss()
        super.onDestroy()
    }

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun showTimeUp() {
        overlay.showTimeUp {
            LockState.cooldownUntil = System.currentTimeMillis() + 5000
            LockState.isLocked = false
            LockState.lockedPackage = ""
            goHome()
        }
    }

    companion object {
        @Volatile
        var instance: LockAccessibilityService? = null
            private set
    }
}
