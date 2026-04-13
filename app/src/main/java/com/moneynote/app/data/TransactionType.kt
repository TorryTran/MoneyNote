package com.moneynote.app.data

enum class TransactionType {
    INCOME,
    EXPENSE;

    companion object {
        fun fromName(raw: String?, fallback: TransactionType = EXPENSE): TransactionType {
            if (raw.isNullOrBlank()) return fallback
            return values().firstOrNull { it.name == raw } ?: fallback
        }
    }
}
