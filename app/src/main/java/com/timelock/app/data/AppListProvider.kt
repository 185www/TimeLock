package com.timelock.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Enumerates user-facing (launchable) apps.
 *
 * Mirrors the Mindful reference project: we only look at MAIN/LAUNCHER apps
 * resolved through PackageManager. We never request QUERY_ALL_PACKAGES, so the
 * list respects the user's installed, openable applications only.
 */
class AppListProvider(private val context: Context) {

    fun getLaunchableApps(): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val own = context.packageName
        return pm
            .queryIntentActivities(intent, 0)
            .mapNotNull { resolve ->
                val packageName = resolve.activityInfo.packageName
                if (packageName == own) return@mapNotNull null
                val label = resolve.loadLabel(pm).toString()
                AppEntry(packageName, label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    data class AppEntry(val packageName: String, val label: String)
}
