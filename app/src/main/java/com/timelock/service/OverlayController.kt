package com.timelock.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var currentOverlay: View? = null

    fun showInterceptOverlay(onConfirm: (Int) -> Unit) {
        dismiss()

        val density = context.resources.displayMetrics.density
        fun dp(n: Int) = (n * density).toInt()

        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        root.addView(container)

        container.addView(TextView(context).apply {
            text = "你打算用多久？"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(48))
        })

        var selectedMinutes = 0
        val confirmBtn = Button(context).apply {
            text = "请先选择时长"
            setTextColor(Color.WHITE)
            textSize = 18f
            isEnabled = false
            setBackgroundColor(Color.parseColor("#FF5722"))
            layoutParams = LinearLayout.LayoutParams(dp(280), dp(56))
        }
        confirmBtn.setOnClickListener {
            if (selectedMinutes > 0) {
                onConfirm(selectedMinutes)
                dismiss()
            }
        }

        fun makePresetButton(mins: Int): Button {
            val btn = Button(context).apply {
                text = "${mins}分钟"
                setTextColor(Color.WHITE)
                textSize = 16f
                setBackgroundColor(Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(dp(88), dp(48)).apply {
                    setMargins(dp(6), 0, dp(6), 0)
                }
                setOnClickListener {
                    selectedMinutes = mins
                    confirmBtn.text = "确认 — 使用 ${mins} 分钟"
                    confirmBtn.isEnabled = true
                    confirmBtn.setBackgroundColor(Color.parseColor("#4CAF50"))
                }
            }
            return btn
        }

        fun addRow(minsList: List<Int>) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(0, 0, 0, dp(12))
                }
            }
            for (m in minsList) row.addView(makePresetButton(m))
            container.addView(row)
        }

        addRow(listOf(5, 10, 15))
        addRow(listOf(30, 60, 90))
        addRow(listOf(120))

        container.addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(32))
        })

        container.addView(confirmBtn)

        container.addView(TextView(context).apply {
            text = "锁定期间无法跳过"
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        })

        val params = WindowManager.LayoutParams(
            -1, -1,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.FILL }

        windowManager.addView(root, params)
        currentOverlay = root
    }

    fun showTimeUpOverlay(onDismiss: () -> Unit) {
        dismiss()

        val density = context.resources.displayMetrics.density
        fun dp(n: Int) = (n * density).toInt()

        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        root.addView(container)

        container.addView(TextView(context).apply {
            text = "⏰"
            textSize = 72f
            gravity = Gravity.CENTER
        })

        container.addView(TextView(context).apply {
            text = "时间到啦"
            textSize = 36f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(48))
        })

        Button(context).apply {
            text = "好的"
            setTextColor(Color.WHITE)
            textSize = 20f
            setBackgroundColor(Color.parseColor("#E53935"))
            layoutParams = LinearLayout.LayoutParams(dp(280), dp(56))
            setOnClickListener { onDismiss() }
        }.also { container.addView(it) }

        val params = WindowManager.LayoutParams(
            -1, -1,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.FILL }

        windowManager.addView(root, params)
        currentOverlay = root
    }

    fun dismiss() {
        currentOverlay?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        currentOverlay = null
    }

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }
}
