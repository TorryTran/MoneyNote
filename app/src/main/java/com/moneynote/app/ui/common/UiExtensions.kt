package com.moneynote.app.ui.common

import android.graphics.Color

fun parseColorOrDefault(color: String, fallback: Int): Int {
    return try {
        Color.parseColor(color)
    } catch (_: Exception) {
        fallback
    }
}
