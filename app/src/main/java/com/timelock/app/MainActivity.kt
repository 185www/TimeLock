package com.timelock.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.timelock.app.data.SessionRepository
import com.timelock.app.service.TimeLockAccessibilityService
import com.timelock.app.ui.AppSelectScreen
import com.timelock.app.ui.HomeScreen
import com.timelock.app.ui.theme.TimeLockTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repo by lazy { SessionRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotifications()
        setContent {
            TimeLockTheme {
                val session by repo.session.collectAsStateWithLifecycle(initialValue = null)
                val monitored by repo.monitoredApps.collectAsStateWithLifecycle(initialValue = emptySet())
                var screen by remember { mutableStateOf("home") }
                var accEnabled by remember {
                    mutableStateOf(isAccessibilityEnabled())
                }

                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0B0F17)),
                ) {
                    when (screen) {
                        "select" -> AppSelectScreen(
                            current = monitored,
                            onBack = { screen = "home" },
                            onSave = { set ->
                                lifecycleScope.launch { repo.setMonitoredApps(set) }
                                screen = "home"
                            },
                        )
                        else -> HomeScreen(
                            session = session,
                            monitoredCount = monitored.size,
                            accessibilityEnabled = accEnabled,
                            onOpenSettings = {
                                accEnabled = isAccessibilityEnabled()
                                if (!accEnabled) openAccessibilitySettings() else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "无障碍权限已开启",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                            onSelectApps = { screen = "select" },
                        )
                    }
                }
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, TimeLockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.split(":")?.any { it.equals(expected, ignoreCase = true) } ?: false
    }

    private fun openAccessibilitySettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }
}
