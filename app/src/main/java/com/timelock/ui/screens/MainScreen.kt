package com.timelock.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelock.data.AppRepository
import com.timelock.data.SelectedApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<SelectedApp>>(emptyList()) }

    LaunchedEffect(Unit) {
        apps = AppRepository.loadInstalledApps(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("时间锁", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "选择要监控的应用：",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "当被选中的应用启动时，时间锁会强制你设定使用时长。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Accessibility service check
            var accessibilityEnabled by remember {
                mutableStateOf(isAccessibilityServiceEnabled(context))
            }

            if (!accessibilityEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "需要开启无障碍服务",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "时间锁需要无障碍服务权限来检测应用启动和强制回到桌面。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                )
                            }
                        ) {
                            Text("去开启")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppItem(
                        app = app,
                        onToggle = { selected ->
                            AppRepository.togglePackage(app.packageName, selected)
                            apps = apps.map {
                                if (it.packageName == app.packageName) {
                                    it.copy(isSelected = selected)
                                } else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    app: SelectedApp,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val service = "${context.packageName}/.service.LockAccessibilityService"
    try {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    } catch (_: Exception) {
        return false
    }
}
