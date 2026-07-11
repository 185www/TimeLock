package com.timelock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.timelock.data.AppRepository
import com.timelock.data.LockState

class LockAccessibilityService : AccessibilityService() {

    private lateinit var overlay: OverlayController

    override fun onCreate() {
        super.onCreate()
        overlay = OverlayController(this)
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
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

            overlay.showInterceptOverlay { minutes ->
                val seconds = minutes * 60L
                LockState.isIntercepting = false
                LockState.isTimerRunning = true
                LockState.isLocked = true
                LockState.remainingSeconds = seconds

                val intent = Intent(this, CountdownService::class.java).apply {
                    putExtra("duration_seconds", seconds)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }

    override fun onInterrupt() {}

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
