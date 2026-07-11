package com.timelock.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelock.data.AppRepository
import com.timelock.data.SelectedApp

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<SelectedApp>>(emptyList()) }
    val monitoredCount = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        AppRepository.init(context)
        apps = AppRepository.loadInstalledApps(context)
        monitoredCount.value = apps.count { it.isSelected }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "时间锁",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "已锁定 $monitoredCount 个应用",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        PermissionRow(context)

        Text(
            text = "选择要限制的应用",
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(apps) { app ->
                AppRow(
                    app = app,
                    onToggle = { selected ->
                        AppRepository.togglePackage(app.packageName, selected)
                        monitoredCount.value = if (selected) monitoredCount.value + 1
                        else monitoredCount.value - 1
                    }
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: SelectedApp, onToggle: (Boolean) -> Unit) {
    var checked by remember(app.packageName) { mutableStateOf(app.isSelected) }
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                checked = !checked
                onToggle(checked)
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = remember(app.packageName) {
            drawableToBitmap(AppRepository.getAppIcon(context, app.packageName))
        }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = app.appName,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = {
                checked = it
                onToggle(it)
            }
        )
    }
}

@Composable
private fun PermissionRow(context: Context) {
    val overlayGranted = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        if (!overlayGranted.value) {
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("授予悬浮窗权限（必需）")
            }
        }
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("开启无障碍服务（必需）")
        }
    }
}

private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable) return drawable.bitmap
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
