package com.timelock.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

object AppRepository {
    private val selectedPackages = mutableSetOf<String>()

    fun loadInstalledApps(context: Context): List<SelectedApp> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)

        val ourPackage = context.packageName
        val appMap = mutableMapOf<String, SelectedApp>()

        for (resolveInfo in resolveInfos) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val packageName = activityInfo.packageName

            if (packageName == ourPackage) continue
            if (packageName in appMap) continue

            val flags = activityInfo.applicationInfo.flags
            val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (isSystem) continue

            val appName = resolveInfo.loadLabel(pm).toString()
            appMap[packageName] = SelectedApp(
                packageName = packageName,
                appName = appName,
                isSelected = packageName in selectedPackages
            )
        }

        return appMap.values.sortedBy { it.appName }
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
