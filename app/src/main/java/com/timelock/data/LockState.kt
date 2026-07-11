package com.timelock.data

object LockState {
    var isLocked: Boolean = false
    var lockedPackage: String = ""
    var remainingSeconds: Long = 0L
    var isIntercepting: Boolean = false
    var isTimerRunning: Boolean = false

    fun reset() {
        isLocked = false
        lockedPackage = ""
        remainingSeconds = 0L
        isIntercepting = false
        isTimerRunning = false
    }
}
