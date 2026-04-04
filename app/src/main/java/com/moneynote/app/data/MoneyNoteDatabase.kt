package com.moneynote.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

class MoneyNoteDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRANSACTIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                wallet TEXT NOT NULL DEFAULT 'Tiền mặt',
                category TEXT NOT NULL,
                note TEXT,
                date INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_transactions_date ON $TABLE_TRANSACTIONS(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN wallet TEXT NOT NULL DEFAULT 'Tiền mặt'")
        }
    }

    fun insert(tx: TransactionEntity): Long {
        val values = ContentValues().apply {
            put("type", tx.type.name)
            put("amount", tx.amount)
            put("wallet", tx.wallet)
            put("category", tx.category)
            put("note", tx.note)
            put("date", tx.date)
        }
        return writableDatabase.insert(TABLE_TRANSACTIONS, null, values)
    }

    fun update(tx: TransactionEntity): Int {
        val values = ContentValues().apply {
            put("type", tx.type.name)
            put("amount", tx.amount)
            put("wallet", tx.wallet)
            put("category", tx.category)
            put("note", tx.note)
            put("date", tx.date)
        }
        return writableDatabase.update(
            TABLE_TRANSACTIONS,
            values,
            "id=?",
            arrayOf(tx.id.toString())
        )
    }

    fun delete(id: Long): Int {
        return writableDatabase.delete(TABLE_TRANSACTIONS, "id=?", arrayOf(id.toString()))
    }

    fun getTransactionsByDay(dayStart: Long, dayEnd: Long): List<TransactionEntity> {
        val sql = "SELECT * FROM $TABLE_TRANSACTIONS WHERE date >= ? AND date <= ? ORDER BY date DESC"
        val cursor = readableDatabase.rawQuery(sql, arrayOf(dayStart.toString(), dayEnd.toString()))
        return cursor.use {
            val result = mutableListOf<TransactionEntity>()
            while (it.moveToNext()) {
                result.add(
                    TransactionEntity(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        type = TransactionType.valueOf(it.getString(it.getColumnIndexOrThrow("type"))),
                        amount = it.getLong(it.getColumnIndexOrThrow("amount")),
                        wallet = it.getString(it.getColumnIndexOrThrow("wallet")) ?: "Tiền mặt",
                        category = it.getString(it.getColumnIndexOrThrow("category")),
                        note = it.getString(it.getColumnIndexOrThrow("note")) ?: "",
                        date = it.getLong(it.getColumnIndexOrThrow("date"))
                    )
                )
            }
            result
        }
    }

    fun getMonthSummary(monthStart: Long, monthEnd: Long): MonthSummary {
        val sql = "SELECT type, SUM(amount) AS total FROM $TABLE_TRANSACTIONS WHERE date >= ? AND date <= ? GROUP BY type"
        val cursor = readableDatabase.rawQuery(sql, arrayOf(monthStart.toString(), monthEnd.toString()))

        var income = 0L
        var expense = 0L
        cursor.use {
            while (it.moveToNext()) {
                val type = it.getString(it.getColumnIndexOrThrow("type"))
                val total = it.getLong(it.getColumnIndexOrThrow("total"))
                if (type == TransactionType.INCOME.name) income = total else expense = total
            }
        }
        return MonthSummary(income, expense)
    }

    fun getDaySummariesInMonth(monthStart: Long, monthEnd: Long): Map<Int, DaySummary> {
        val sql =
            "SELECT date, type, SUM(amount) AS total FROM $TABLE_TRANSACTIONS WHERE date >= ? AND date <= ? GROUP BY date(date/1000,'unixepoch','localtime'), type"
        val cursor = readableDatabase.rawQuery(sql, arrayOf(monthStart.toString(), monthEnd.toString()))
        val map = mutableMapOf<Int, DaySummary>()
        val cal = Calendar.getInstance()

        cursor.use {
            while (it.moveToNext()) {
                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                val type = it.getString(it.getColumnIndexOrThrow("type"))
                val total = it.getLong(it.getColumnIndexOrThrow("total"))

                cal.timeInMillis = date
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val old = map[day] ?: DaySummary(day, 0, 0)
                map[day] = if (type == TransactionType.INCOME.name) {
                    old.copy(incomeTotal = old.incomeTotal + total)
                } else {
                    old.copy(expenseTotal = old.expenseTotal + total)
                }
            }
        }
        return map
    }

    fun getAllTransactions(): List<TransactionEntity> {
        val sql = "SELECT * FROM $TABLE_TRANSACTIONS ORDER BY date DESC, id DESC"
        val cursor = readableDatabase.rawQuery(sql, null)
        return cursor.use {
            val result = mutableListOf<TransactionEntity>()
            while (it.moveToNext()) {
                result.add(
                    TransactionEntity(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        type = TransactionType.valueOf(it.getString(it.getColumnIndexOrThrow("type"))),
                        amount = it.getLong(it.getColumnIndexOrThrow("amount")),
                        wallet = it.getString(it.getColumnIndexOrThrow("wallet")) ?: "Tiền mặt",
                        category = it.getString(it.getColumnIndexOrThrow("category")),
                        note = it.getString(it.getColumnIndexOrThrow("note")) ?: "",
                        date = it.getLong(it.getColumnIndexOrThrow("date"))
                    )
                )
            }
            result
        }
    }

    fun replaceAllTransactions(items: List<TransactionEntity>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete(TABLE_TRANSACTIONS, null, null)
            items.forEach { tx ->
                val values = ContentValues().apply {
                    put("type", tx.type.name)
                    put("amount", tx.amount)
                    put("wallet", tx.wallet)
                    put("category", tx.category)
                    put("note", tx.note)
                    put("date", tx.date)
                }
                writableDatabase.insert(TABLE_TRANSACTIONS, null, values)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    companion object {
        private const val DB_NAME = "moneynote.db"
        private const val DB_VERSION = 2
        private const val TABLE_TRANSACTIONS = "transactions"

        @Volatile
        private var INSTANCE: MoneyNoteDatabase? = null

        fun getInstance(context: Context): MoneyNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MoneyNoteDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
