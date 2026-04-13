package com.moneynote.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionRepository private constructor(context: Context) {
    private val db = MoneyNoteDatabase.getInstance(context)

    suspend fun add(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        db.insert(transaction).also { DataChangeTracker.bumpTransactions() }
    }

    suspend fun update(transaction: TransactionEntity): Int = withContext(Dispatchers.IO) {
        db.update(transaction).also { DataChangeTracker.bumpTransactions() }
    }

    suspend fun delete(id: Long): Int = withContext(Dispatchers.IO) {
        db.delete(id).also { DataChangeTracker.bumpTransactions() }
    }

    suspend fun getByDay(dayStart: Long, dayEnd: Long): List<TransactionEntity> =
        withContext(Dispatchers.IO) {
            db.getTransactionsByDay(dayStart, dayEnd)
        }

    suspend fun getMonthSummary(monthStart: Long, monthEnd: Long): MonthSummary =
        withContext(Dispatchers.IO) {
            db.getMonthSummary(monthStart, monthEnd)
        }

    suspend fun getMonthDaySummary(monthStart: Long, monthEnd: Long): Map<Int, DaySummary> =
        withContext(Dispatchers.IO) {
            db.getDaySummariesInMonth(monthStart, monthEnd)
        }

    suspend fun getAll(): List<TransactionEntity> = withContext(Dispatchers.IO) {
        db.getAllTransactions()
    }

    suspend fun replaceAll(items: List<TransactionEntity>) = withContext(Dispatchers.IO) {
        db.replaceAllTransactions(items)
        DataChangeTracker.bumpTransactions()
    }

    companion object {
        @Volatile
        private var INSTANCE: TransactionRepository? = null

        fun get(context: Context): TransactionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TransactionRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
