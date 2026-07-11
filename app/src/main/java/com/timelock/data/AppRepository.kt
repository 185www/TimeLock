package com.timelock.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

object AppRepository {
    private val selectedPackages = mutableSetOf<String>()

    fun loadInstalledApps(context: Context): List<SelectedApp> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val ourPackage = context.packageName
        val appMap = mutableMapOf<String, SelectedApp>()

        for (resolveInfo in resolveInfos) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val packageName = activityInfo.packageName
            if (packageName == ourPackage || packageName in appMap) continue

            val flags = activityInfo.applicationInfo.flags
            if ((flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue

            val appName = resolveInfo.loadLabel(pm).toString()
            val icon = drawableToBitmap(resolveInfo.loadIcon(pm))

            appMap[packageName] = SelectedApp(
                packageName = packageName,
                appName = appName,
                icon = icon,
                isSelected = packageName in selectedPackages
            )
        }

        return appMap.values.sortedBy { it.appName }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(48),
                drawable.intrinsicHeight.coerceAtLeast(48),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun togglePackage(packageName: String, selected: Boolean) {
        if (selected) selectedPackages.add(packageName)
        else selectedPackages.remove(packageName)
    }

    fun isMonitored(packageName: String): Boolean = packageName in selectedPackages
    fun getMonitoredPackages(): Set<String> = selectedPackages.toSet()
}
