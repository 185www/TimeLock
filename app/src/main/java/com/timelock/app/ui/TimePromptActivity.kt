package com.timelock.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.timelock.app.data.SessionRepository
import com.timelock.app.service.CountdownService
import com.timelock.app.ui.theme.TimeLockTheme
import kotlinx.coroutines.launch

/**
 * Full-screen dialog launched on top of a monitored app when it is opened.
 * The user must pick a usage time before they can use the app; choosing
 * "退出" kicks them out without starting a session.
 */
class TimePromptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge()
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (packageName.isNullOrEmpty()) {
            finish()
            return
        }
        val appLabel = resolveLabel(packageName)
        setContent {
            TimeLockTheme {
                TimePromptScreen(
                    appLabel = appLabel,
                    onStart = { minutes -> confirm(packageName, minutes) },
                    onQuit = { quit() },
                )
            }
        }
    }

    private fun resolveLabel(packageName: String): String = runCatching {
        val pm = packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    private fun confirm(packageName: String, minutes: Long) {
        val endTime = System.currentTimeMillis() + minutes * 60_000L
        lifecycleScope.launch {
            SessionRepository(this@TimePromptActivity).startSession(packageName, endTime)
            CountdownService.startIfNeeded(this@TimePromptActivity, packageName, endTime)
            finish()
        }
    }

    private fun quit() {
        goHome()
        finish()
    }

    private fun goHome() {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        } catch (_: ActivityNotFoundException) {
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }
}

@Composable
private fun TimePromptScreen(
    appLabel: String,
    onStart: (Long) -> Unit,
    onQuit: () -> Unit,
) {
    var selectedMinutes by remember { mutableStateOf<Long?>(null) }
    var customMinutes by remember { mutableStateOf("") }
    var showCustom by remember { mutableStateOf(false) }

    val presets = listOf(5L to "5 分钟", 15L to "15 分钟", 30L to "30 分钟", 60L to "1 小时", 120L to "2 小时")

    val effective = selectedMinutes ?: customMinutes.toLongOrNull()
    val valid = effective != null && effective > 0L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "设定使用时间",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "你打开了 $appLabel，想用多久？",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            presets.forEach { (minutes, label) ->
                val isSel = selectedMinutes == minutes
                Button(
                    onClick = { selectedMinutes = minutes; showCustom = false; customMinutes = "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF1A2230),
                        contentColor = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(label, fontSize = 17.sp)
                }
            }

            TextButton(onClick = { showCustom = !showCustom }) {
                Text(if (showCustom) "收起自定义" else "自定义时间", color = MaterialTheme.colorScheme.primary)
            }

            AnimatedVisibility(visible = showCustom, enter = fadeIn(), exit = fadeOut()) {
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { v ->
                        if (v.all { it.isDigit() } && v.length <= 4) {
                            customMinutes = v
                            selectedMinutes = null
                        }
                    },
                    label = { Text("分钟") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .size(220.dp, 60.dp)
                        .padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { effective?.let(onStart) },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF1A2230),
                    disabledContentColor = Color.Gray,
                ),
            ) {
                Text(if (valid) "开始使用" else "请选择一个时间", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onQuit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("不使用，退出", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
