package com.moneynote.app.data

data class TransactionEntity(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val wallet: String = "Tiền mặt",
    val category: String,
    val note: String,
    val date: Long
)
