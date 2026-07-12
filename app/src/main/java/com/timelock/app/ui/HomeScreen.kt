package com.timelock.app.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelock.app.data.SessionRepository
import com.timelock.app.service.formatDuration

@Composable
fun HomeScreen(
    session: SessionRepository.Session?,
    monitoredCount: Int,
    accessibilityEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onSelectApps: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "时间锁",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "打开受限应用 → 设定使用时间 → 时间到自动退出",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        // Accessibility status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = if (accessibilityEnabled) "无障碍权限已开启" else "无障碍权限未开启",
                    color = if (accessibilityEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "时间锁需要通过“无障碍”检测你正在使用的应用，才能在应用打开时弹出时间设置，并在时间到后自动退出。",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(if (accessibilityEnabled) "重新打开无障碍设置" else "去开启无障碍权限")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Current session status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = if (session != null) {
                        val label = appLabel(context, session.packageName)
                        "剩余 ${formatDuration((session.endTime - System.currentTimeMillis()).coerceAtLeast(0L))}　（$label）"
                    } else {
                        "当前没有正在计时的应用"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onSelectApps,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
            ),
        ) {
            Text(
                text = if (monitoredCount > 0) "管理受限应用（已选 $monitoredCount 个）" else "选择要限制的应用",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "提示：勾选你容易沉迷的应用。打开它们时会先让你设定本次可用时间；时间一到，时间锁会自动退出该应用。",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

private fun appLabel(context: Context, packageName: String): String = runCatching {
    val pm = context.packageManager
    val info = pm.getApplicationInfo(packageName, 0)
    pm.getApplicationLabel(info).toString()
}.getOrDefault(packageName)
