package com.timelock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.timelock.data.AppRepository
import com.timelock.data.LockState
import com.timelock.ui.screens.InterceptActivity

class LockAccessibilityService : AccessibilityService() {

    private var lastPackage = ""

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == null) return

        val packageName = event.packageName.toString()

        if (packageName == lastPackage) return
        lastPackage = packageName

        if (LockState.isTimerRunning) return
        if (LockState.isIntercepting) return
        if (packageName == "com.timelock") return

        if (AppRepository.isMonitored(packageName)) {
            LockState.lockedPackage = packageName
            LockState.isIntercepting = true

            val intent = Intent(this, InterceptActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
