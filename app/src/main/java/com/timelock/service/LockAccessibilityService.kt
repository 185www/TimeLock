package com.timelock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.timelock.data.AppRepository
import com.timelock.data.LockState

class LockAccessibilityService : AccessibilityService() {

    private var lastPackage: String = ""
    private var lastEventTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppRepository.init(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            if (packageName == lastPackage) return
            if (packageName.isEmpty()) return
            if (packageName == this.packageName) return

            val now = System.currentTimeMillis()
            if (now - lastEventTime < 500) return

            lastPackage = packageName
            lastEventTime = now

            if (!AppRepository.isMonitored(packageName)) return
            if (!LockState.canIntercept(packageName)) return

            val intent = Intent(this, TrackerService::class.java).apply {
                action = TrackerService.ACTION_INTERCEPT
                putExtra(TrackerService.EXTRA_PACKAGE, packageName)
            }
            LockState.isIntercepting = true
            LockState.lockedPackage = packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    override fun onInterrupt() {}
}
