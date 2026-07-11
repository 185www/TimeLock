package com.timelock.data

data class SelectedApp(
    val packageName: String,
    val appName: String,
    val isSelected: Boolean = false
)
