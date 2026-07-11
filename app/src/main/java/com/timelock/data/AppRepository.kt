package com.timelock.data

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.Intent
import java.util.Collections

object AppRepository {
    private val selectedPackages = mutableSetOf<String>()

    fun loadInstalledApps(context: Context): List<SelectedApp> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        }

        val systemPackages = setOf(
            "android",
            "com.android.settings",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            context.packageName
        )

        val appMap = mutableMapOf<String, SelectedApp>()
        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName in systemPackages) continue
            if (packageName in appMap) continue
            val appName = resolveInfo.loadLabel(pm).toString()
            appMap[packageName] = SelectedApp(
                packageName = packageName,
                appName = appName,
                isSelected = packageName in selectedPackages
            )
        }

        val sorted = appMap.values.sortedBy { it.appName }
        return sorted
    }

    fun togglePackage(packageName: String, selected: Boolean) {
        if (selected) {
            selectedPackages.add(packageName)
        } else {
            selectedPackages.remove(packageName)
        }
    }

    fun isMonitored(packageName: String): Boolean {
        return packageName in selectedPackages
    }

    fun getMonitoredPackages(): Set<String> {
        return selectedPackages.toSet()
    }
}
