package com.timelock.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timelock.data.LockState
import com.timelock.service.CountdownService

class InterceptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            InterceptScreen(
                onConfirm = { minutes ->
                    val seconds = minutes * 60L
                    val intent = Intent(this@InterceptActivity, CountdownService::class.java).apply {
                        putExtra("duration_seconds", seconds)
                    }
                    startService(intent)
                    LockState.isIntercepting = false
                    finish()
                }
            )
        }
    }

    override fun onBackPressed() {
        // Block back press during intercept
    }
}

@Composable
private fun InterceptScreen(onConfirm: (Int) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
            onValueChange = { value ->
                inputText = value.filter { it.isDigit() }
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
            Text(
                "确认 — 使用 ${minutes} 分钟",
                fontSize = 20.sp,
                color = Color.White
            )
        }
    }
}
