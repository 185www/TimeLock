package com.timelock.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.timelock.data.AppRepository
import com.timelock.data.SelectedApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var apps by remember { mutableStateOf<List<SelectedApp>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val accessibilityEnabled = remember { mutableStateOf(isAccessibilityEnabled(context)) }
    val batteryOptimizationDisabled = remember { mutableStateOf(isBatteryOptimizationDisabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled.value = isAccessibilityEnabled(context)
                batteryOptimizationDisabled.value = isBatteryOptimizationDisabled(context)
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshTrigger) {
        apps = AppRepository.loadInstalledApps(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时间锁", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ---- Permission section ----
            item {
                Text(
                    text = "权限与后台保护",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                )
            }

            if (!accessibilityEnabled.value) {
                item {
                    PermissionCard(
                        icon = Icons.Default.Security,
                        title = "无障碍服务",
                        description = "必须开启：时间锁通过无障碍服务检测应用启动并强制拦截",
                        buttonText = "去开启" to { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        isWarning = true
                    )
                }
            } else {
                item {
                    PermissionCard(
                        icon = Icons.Default.Security,
                        title = "无障碍服务",
                        description = "已开启 ✓",
                        buttonText = null,
                        isWarning = false
                    )
                }
            }

            if (!batteryOptimizationDisabled.value) {
                item {
                    PermissionCard(
                        icon = Icons.Default.BatteryChargingFull,
                        title = "忽略电池优化",
                        description = "防止系统杀掉倒计时后台服务",
                        buttonText = "去设置" to {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        isWarning = true
                    )
                }
            } else {
                item {
                    PermissionCard(
                        icon = Icons.Default.BatteryChargingFull,
                        title = "忽略电池优化",
                        description = "已允许 ✓",
                        buttonText = null,
                        isWarning = false
                    )
                }
            }

            item {
                val notifWarning = Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission(context)
                PermissionCard(
                    icon = Icons.Default.Notifications,
                    title = "前台通知",
                    description = if (notifWarning) "未授权" else "倒计时期间显示通知，保持服务存活",
                    buttonText = if (notifWarning) "去开启" to { openNotificationSettings(context) } else null,
                    isWarning = notifWarning
                )
            }

            // ---- Overlay permission (needed for background activity launch on Android 12+) ----
            item {
                val hasOverlay = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)
                PermissionCard(
                    icon = Icons.Default.Security,
                    title = "悬浮窗权限",
                    description = if (hasOverlay) "已授权 ✓" else "部分设备需要悬浮窗权限才能弹出拦截界面",
                    buttonText = if (hasOverlay) null else "去开启" to {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    isWarning = !hasOverlay
                )
            }

            // ---- Auto start (MIUI/EMUI/ColorOS) ----
            item {
                PermissionCard(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "自启动",
                    description = "MIUI/EMUI/ColorOS 等系统需允许自启动，否则后台服务会被杀掉",
                    buttonText = "去设置" to {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    isWarning = false
                )
            }

            // ---- Divider + app list header ----
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择要监控的应用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (apps.isEmpty()) {
                        Text(
                            text = "未读取到应用，请检查权限",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
                if (apps.isNotEmpty()) {
                    Text(
                        text = "共 ${apps.size} 个应用 · 已选 ${apps.count { it.isSelected }} 个",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (apps.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "无法读取已安装应用",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "请在系统设置 → 应用 → 特殊权限 → 安装未知应用 中允许时间锁查询应用列表。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            items(apps, key = { it.packageName }) { app ->
                AppItem(
                    app = app,
                    onToggle = { selected ->
                        AppRepository.togglePackage(app.packageName, selected)
                        apps = apps.map {
                            if (it.packageName == app.packageName) it.copy(isSelected = selected)
                            else it
                        }
                    }
                )
            }

            // Bottom padding
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: Pair<String, () -> Unit>?,
    isWarning: Boolean,
    warningMessage: String = ""
) {
    val bgColor = if (isWarning)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isWarning && warningMessage.isNotEmpty()) warningMessage else description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            if (buttonText != null) {
                TextButton(onClick = buttonText.second) {
                    Text(
                        buttonText.first,
                        color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = "✓",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun AppItem(
    app: SelectedApp,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon.asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = app.isSelected,
                onCheckedChange = onToggle
            )
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val shortForm = "${context.packageName}/.service.LockAccessibilityService"
    val longForm = "${context.packageName}/${context.packageName}.service.LockAccessibilityService"
    return try {
        val services = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        services.contains(shortForm) || services.contains(longForm)
    } catch (_: Exception) {
        false
    }
}

private fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return try {
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } catch (_: Exception) {
        false
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return try {
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }
}

private fun openNotificationSettings(context: Context) {
    if (Build.VERSION.SDK_INT < 33) return
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}
