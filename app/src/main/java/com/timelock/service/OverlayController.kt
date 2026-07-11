package com.timelock.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun showIntercept(onConfirm: (Int) -> Unit) {
        dismiss()

        val composeView = ComposeView(context).apply {
            setContent {
                InterceptOverlay(onConfirm = { minutes ->
                    onConfirm(minutes)
                    dismiss()
                })
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(composeView, params)
        overlayView = composeView
    }

    fun showTimeUp(onDismiss: () -> Unit) {
        dismiss()

        val composeView = ComposeView(context).apply {
            setContent {
                TimeUpOverlay(onDismiss = {
                    onDismiss()
                    dismiss()
                })
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(composeView, params)
        overlayView = composeView
    }

    fun dismiss() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    fun isShowing(): Boolean = overlayView != null
}

@Composable
private fun InterceptOverlay(onConfirm: (Int) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "你打算用多久？",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(5, 15, 30, 60).forEach { mins ->
                val isSelected = selectedMinutes == mins
                Button(
                    onClick = {
                        selectedMinutes = mins
                        inputText = mins.toString()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color.White else Color.DarkGray,
                        contentColor = if (isSelected) Color.Black else Color.White
                    )
                ) {
                    Text("${mins}分钟", fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it.filter { c -> c.isDigit() }
                selectedMinutes = -1
            },
            label = { Text("自定义分钟", color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        val minutes = inputText.toIntOrNull() ?: 0
        Button(
            onClick = { onConfirm(minutes) },
            enabled = minutes > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color.DarkGray
            )
        ) {
            Text("确认 — 使用 ${minutes} 分钟", fontSize = 20.sp, color = Color.White)
        }

        Text(
            text = "锁定期间无法退出",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun TimeUpOverlay(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⏰", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "时间到啦",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
        ) {
            Text("好的", fontSize = 20.sp, color = Color.White)
        }
    }
}
