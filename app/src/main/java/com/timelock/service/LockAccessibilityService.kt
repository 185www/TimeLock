package com.timelock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.timelock.data.AppRepository
import com.timelock.data.LockState
import com.timelock.ui.screens.InterceptActivity

class LockAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

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
        if (packageName == "com.timelock") return
        if (LockState.isTimerRunning || LockState.isIntercepting) return

        if (AppRepository.isMonitored(packageName)) {
            LockState.lockedPackage = packageName
            LockState.isIntercepting = true

            // Go home first so our activity can appear on top reliably
            performGlobalAction(GLOBAL_ACTION_HOME)

            handler.postDelayed({
                try {
                    val intent = Intent(this, InterceptActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        )
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    LockState.isIntercepting = false
                }
            }, 150)
        }
    }

    override fun onInterrupt() {}

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
