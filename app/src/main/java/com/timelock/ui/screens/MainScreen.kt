package com.timelock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelock.ui.theme.LockDark
import com.timelock.ui.theme.LockRed
import com.timelock.ui.theme.TimerGreen
import kotlinx.coroutines.delay

@Composable
fun MainScreen() {
    var isLocked by remember { mutableStateOf(false) }
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(25) }
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }

    if (isLocked) {
        LockScreen(
            remainingSeconds = remainingSeconds,
            onUnlock = {
                isLocked = false
                isTimerRunning = false
            }
        )
    } else {
        TimePickerScreen(
            hours = hours,
            minutes = minutes,
            onHoursChange = { hours = it },
            onMinutesChange = { minutes = it },
            isTimerRunning = isTimerRunning,
            onStart = {
                remainingSeconds = (hours * 3600L + minutes * 60L)
                isLocked = true
                isTimerRunning = true
            }
        )
    }
}

@Composable
fun TimePickerScreen(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    isTimerRunning: Boolean,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "时间锁",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "设定锁定时间",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    repeat(10) { i ->
                        if (i < 10) {
                            TextButton(onClick = { onHoursChange(i) }) {
                                Text("$i", fontSize = 20.sp)
                            }
                        }
                    }
                }
                Text("小时", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    repeat(12) { i ->
                        val m = i * 5
                        TextButton(onClick = { onMinutesChange(m) }) {
                            Text("$m", fontSize = 16.sp)
                        }
                    }
                }
                Text("分钟", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "${hours}小时 ${minutes}分钟",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStart,
            enabled = !isTimerRunning && (hours > 0 || minutes > 0),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LockRed)
        ) {
            Text("开始锁定", fontSize = 18.sp)
        }
    }
}

@Composable
fun LockScreen(
    remainingSeconds: Long,
    onUnlock: () -> Unit
) {
    var seconds by remember(remainingSeconds) { mutableLongStateOf(remainingSeconds) }

    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1000L)
            seconds--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LockDark)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔒",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "专注中",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(modifier = Modifier.height(48.dp))

        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        val timeStr = String.format("%02d:%02d:%02d", h, m, s)

        Text(
            text = timeStr,
            fontSize = 56.sp,
            fontFamily = FontFamily.Monospace,
            color = if (seconds > 0) TimerGreen else LockRed,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (seconds <= 0) {
            Text(
                text = "时间到！",
                style = MaterialTheme.typography.headlineMedium,
                color = TimerGreen
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TimerGreen)
            ) {
                Text("解锁", fontSize = 18.sp)
            }
        }
    }
}
