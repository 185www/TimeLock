package com.timelock.data

import android.graphics.Bitmap

data class SelectedApp(
    val packageName: String,
    val appName: String,
    val icon: Bitmap? = null,
    val isSelected: Boolean = false
)
