package com.timelock.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.timelock.R

class OverlayController(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val baseParams: WindowManager.LayoutParams
        get() {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            return WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
        }

    private var interceptView: View? = null
    private var countdownView: View? = null
    private var timeUpView: View? = null
    private var countdownText: TextView? = null

    private val countdownParams: WindowManager.LayoutParams
        get() = baseParams.apply {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

    fun haveOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    fun showIntercept(appName: String, onConfirm: (Int) -> Unit) {
        if (interceptView != null) return
        if (!haveOverlayPermission()) return

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_intercept, null)
        view.findViewById<TextView>(R.id.interceptAppName).text = appName

        val minutes = listOf(5, 10, 15, 30, 60, 120)
        val ids = listOf(
            R.id.btn5, R.id.btn10, R.id.btn15,
            R.id.btn30, R.id.btn60, R.id.btn120
        )
        ids.forEachIndexed { index, id ->
            view.findViewById<Button>(id).setOnClickListener {
                onConfirm(minutes[index])
            }
        }

        windowManager.addView(view, baseParams)
        interceptView = view
    }

    fun hideIntercept() {
        interceptView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
            interceptView = null
        }
    }

    fun showCountdown(onGiveUp: () -> Unit) {
        if (countdownView != null) return
        if (!haveOverlayPermission()) return

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_countdown, null)
        countdownText = view.findViewById(R.id.countdownText)
        view.findViewById<Button>(R.id.btnGiveUp).setOnClickListener { onGiveUp() }

        windowManager.addView(view, countdownParams)
        countdownView = view
    }

    fun updateCountdown(seconds: Long) {
        countdownText?.text = formatTime(seconds)
    }

    fun hideCountdown() {
        countdownView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
            countdownView = null
            countdownText = null
        }
    }

    fun showTimeUp(appName: String, onHome: () -> Unit) {
        if (timeUpView != null) return
        if (!haveOverlayPermission()) return

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_timeup, null)
        view.findViewById<TextView>(R.id.timeUpAppName).text = "「$appName」使用时间已结束"
        view.findViewById<Button>(R.id.btnHome).setOnClickListener { onHome() }

        windowManager.addView(view, baseParams)
        timeUpView = view
    }

    fun hideTimeUp() {
        timeUpView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
            timeUpView = null
        }
    }

    fun removeAll() {
        hideIntercept()
        hideCountdown()
        hideTimeUp()
    }

    private fun formatTime(seconds: Long): String {
        val s = if (seconds < 0) 0 else seconds
        val m = s / 60
        val sec = s % 60
        return "%02d:%02d".format(m, sec)
    }
}
