package com.moneynote.app.data

data class TransactionEntity(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val wallet: String = "Tiền mặt",
    val transferToWallet: String = "",
    val isTransfer: Boolean = false,
    val category: String,
    val note: String,
    val date: Long
)
