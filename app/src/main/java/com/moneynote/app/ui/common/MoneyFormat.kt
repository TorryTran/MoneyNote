package com.moneynote.app.ui.common

import java.text.NumberFormat
import java.util.Locale

object MoneyFormat {
    private val formatter = ThreadLocal.withInitial {
        NumberFormat.getInstance(Locale("vi", "VN"))
    }

    fun format(value: Long): String {
        val numberFormat = requireNotNull(formatter.get())
        return numberFormat.format(value) + "đ"
    }
}
