package com.timelock.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timelock.app.data.SessionRepository
import com.timelock.app.service.CountdownService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * After reboot, if a usage session was still active, restart the countdown
 * service so the timer keeps running. The accessibility service enforces the
 * exit when the app is opened again.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            scope.launch {
                val session = SessionRepository(context).getSession()
                if (session != null && session.endTime > System.currentTimeMillis()) {
                    CountdownService.startIfNeeded(context, session.packageName, session.endTime)
                }
            }
        }
    }
}
