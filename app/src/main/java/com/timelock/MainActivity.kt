package com.timelock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.timelock.data.AppRepository
import com.timelock.ui.screens.MainScreen
import com.timelock.ui.theme.TimeLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.init(this)
        setContent {
            TimeLockTheme {
                MainScreen()
            }
        }
    }
}
