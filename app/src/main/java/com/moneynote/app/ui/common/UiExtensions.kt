package com.moneynote.app.ui.common

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.moneynote.app.R

fun parseColorOrDefault(color: String, fallback: Int): Int {
    return try {
        Color.parseColor(color)
    } catch (_: Exception) {
        fallback
    }
}

fun vibrateWarning(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(120L)
    }
}

fun styleAppDialog(dialog: AlertDialog, context: Context) {
    val accent = ContextCompat.getColor(context, R.color.accent_blue)
    val secondary = ContextCompat.getColor(context, R.color.text_secondary)
    dialog.findViewById<TextView>(R.id.alertTitle)?.apply {
        setTextColor(accent)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
    }
    dialog.findViewById<TextView>(android.R.id.message)?.apply {
        setTextColor(secondary)
    }
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
        isAllCaps = false
        setTextColor(accent)
        textSize = 16f
    }
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
        isAllCaps = false
        setTextColor(secondary)
        textSize = 16f
    }
    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
        isAllCaps = false
        setTextColor(accent)
        textSize = 16f
    }
}
