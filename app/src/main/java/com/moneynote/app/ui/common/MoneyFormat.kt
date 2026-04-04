package com.moneynote.app.ui.common

import java.text.NumberFormat
import java.util.Locale

object MoneyFormat {
    private val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

    fun format(value: Long): String {
        return formatter.format(value) + "đ"
    }
}
