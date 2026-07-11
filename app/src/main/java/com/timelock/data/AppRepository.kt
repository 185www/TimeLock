package com.timelock.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

object AppRepository {
    private const val PREFS_NAME = "timelock_prefs"
    private const val KEY_SELECTED = "selected_packages"

    private lateinit var prefs: SharedPreferences
    private val selectedPackages = mutableSetOf<String>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectedPackages.clear()
        selectedPackages.addAll(prefs.getStringSet(KEY_SELECTED, emptySet()) ?: emptySet())
    }

    fun loadInstalledApps(context: Context): List<SelectedApp> {
        if (!::prefs.isInitialized) init(context)
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val ourPackage = context.packageName
        val appMap = linkedMapOf<String, SelectedApp>()

        for (resolveInfo in resolveInfos) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val packageName = activityInfo.packageName
            if (packageName == ourPackage || packageName in appMap) continue
            if ((activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue

            val appName = resolveInfo.loadLabel(pm).toString()
            appMap[packageName] = SelectedApp(
                packageName = packageName,
                appName = appName,
                isSelected = packageName in selectedPackages
            )
        }

        return appMap.values.sortedBy { it.appName }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    fun getAppName(context: Context, packageName: String): String {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    fun togglePackage(packageName: String, selected: Boolean) {
        if (selected) selectedPackages.add(packageName) else selectedPackages.remove(packageName)
        if (::prefs.isInitialized) {
            prefs.edit().putStringSet(KEY_SELECTED, selectedPackages.toSet()).apply()
        }
    }

    fun isMonitored(packageName: String): Boolean = packageName in selectedPackages
    fun getMonitoredPackages(): Set<String> = selectedPackages.toSet()
}
