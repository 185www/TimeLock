package com.timelock.data

object LockState {
    var isLocked: Boolean = false
    var lockedPackage: String = ""
    var remainingSeconds: Long = 0L
    var isIntercepting: Boolean = false
    var isTimerRunning: Boolean = false
    var cooldownUntil: Long = 0L

    fun reset() {
        isLocked = false
        lockedPackage = ""
        remainingSeconds = 0L
        isIntercepting = false
        isTimerRunning = false
    }

    fun canIntercept(packageName: String): Boolean {
        if (isTimerRunning) return false
        if (isIntercepting) return false
        if (packageName == "com.timelock") return false
        if (System.currentTimeMillis() < cooldownUntil) return false
        return true
    }
}
