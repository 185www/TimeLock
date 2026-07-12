package com.timelock.app.service

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.timelock.app.data.SessionRepository
import com.timelock.app.ui.TimePromptActivity
import com.timelock.app.ui.TimeUpActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Detects the foreground app (copied from the Mindful reference: listen for
 * TYPE_WINDOW_STATE_CHANGED and ignore our own package / launcher / system UI).
 *
 * For a monitored app it either:
 *  - launches the time-input prompt (no active session yet), or
 *  - lets it run while a session is active, or
 *  - force-closes it once the session has expired.
 */
class TimeLockAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repo: SessionRepository
    private lateinit var filter: PackageFilter
    private val activityManager by lazy {
        getSystemService(ACTIVITY_SERVICE) as ActivityManager
    }

    @Volatile
    private var currentPackage: String? = null

    // Package we already launched a prompt for, so we don't re-launch on every
    // window-state event while the user is still deciding.
    @Volatile
    private var promptPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = SessionRepository(this)
        filter = PackageFilter(packageName)
        scope.launch { expiryLoop() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (filter.shouldIgnore(pkg)) {
            // Reset tracking when returning to the launcher so the next open of a
            // monitored app is treated as a fresh foreground event.
            if (filter.isLauncher(pkg)) {
                currentPackage = null
                promptPackage = null
            }
            return
        }
        currentPackage = pkg
        scope.launch { handleForeground(pkg) }
    }

    private suspend fun handleForeground(pkg: String) {
        val monitored = repo.getMonitoredApps()
        if (pkg !in monitored) {
            promptPackage = null
            return
        }
        val session = repo.getSession()
        if (session != null && session.packageName == pkg) {
            if (session.endTime <= System.currentTimeMillis()) {
                forceClose(pkg)
            } else {
                promptPackage = null
                CountdownService.startIfNeeded(this, pkg, session.endTime)
            }
            return
        }
        // No active session for this app yet -> ask for the usage time, once.
        if (promptPackage != pkg) {
            promptPackage = pkg
            launchPrompt(pkg)
        }
    }

    private suspend fun expiryLoop() {
        while (isActive) {
            delay(1000)
            val pkg = currentPackage ?: continue
            if (filter.shouldIgnore(pkg)) continue
            if (pkg !in repo.getMonitoredApps()) continue
            val session = repo.getSession() ?: continue
            if (session.packageName == pkg && session.endTime <= System.currentTimeMillis()) {
                forceClose(pkg)
            }
        }
    }

    private fun forceClose(pkg: String) {
        scope.launch { repo.clearSession() }
        promptPackage = null
        currentPackage = null
        // Send the user to the home screen, which exits the foreground app.
        runCatching { performGlobalAction(GLOBAL_ACTION_HOME) }
        // Best-effort kill of the now-backgrounded process.
        runCatching { activityManager.killBackgroundProcesses(pkg) }
        launchTimeUp(pkg)
    }

    private fun launchPrompt(pkg: String) {
        val intent = Intent(this, TimePromptActivity::class.java).apply {
            putExtra(TimePromptActivity.EXTRA_PACKAGE, pkg)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        runCatching { startActivity(intent) }
    }

    private fun launchTimeUp(pkg: String) {
        val intent = Intent(this, TimeUpActivity::class.java).apply {
            putExtra(TimeUpActivity.EXTRA_PACKAGE, pkg)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {
        currentPackage = null
        promptPackage = null
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

/**
 * Ignores our own package, the launcher(s) and system UI — identical to the
 * Mindful reference implementation.
 */
class PackageFilter(
    private val ownPackageName: String,
) {
    private val launcherPackages = setOf(
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oneplus.launcher",
        "com.oppo.launcher",
        "com.vivo.launcher",
    )

    private val ignoredPackages = launcherPackages + setOf(
        "com.android.systemui",
        "android",
    )

    fun shouldIgnore(packageName: String): Boolean =
        packageName == ownPackageName || packageName in ignoredPackages

    fun isLauncher(packageName: String): Boolean = packageName in launcherPackages
}
