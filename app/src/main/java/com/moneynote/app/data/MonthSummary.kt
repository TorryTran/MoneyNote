package com.moneynote.app.data

data class MonthSummary(
    val incomeTotal: Long,
    val expenseTotal: Long
) {
    val balance: Long
        get() = incomeTotal - expenseTotal
}
