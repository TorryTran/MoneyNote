package com.moneynote.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.moneynote.app.ui.entry.WalletItem
import com.moneynote.app.ui.notes.NoteItem
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
                transferToWallet TEXT NOT NULL DEFAULT '',
                isTransfer INTEGER NOT NULL DEFAULT 0,
                category TEXT NOT NULL,
                note TEXT,
                date INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_transactions_date ON $TABLE_TRANSACTIONS(date)")
        db.execSQL(
            """
            CREATE TABLE $TABLE_WALLETS (
                name TEXT PRIMARY KEY,
                balance INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                contentHtml TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notes_updated ON $TABLE_NOTES(updatedAt DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN wallet TEXT NOT NULL DEFAULT 'Tiền mặt'")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN transferToWallet TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_TRANSACTIONS ADD COLUMN isTransfer INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_WALLETS (
                    name TEXT PRIMARY KEY,
                    balance INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_NOTES (
                    id INTEGER PRIMARY KEY,
                    title TEXT NOT NULL,
                    contentHtml TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_notes_updated ON $TABLE_NOTES(updatedAt DESC)")
        }
    }

    fun insert(tx: TransactionEntity): Long {
        val values = ContentValues().apply {
            put("type", tx.type.name)
            put("amount", tx.amount)
            put("wallet", tx.wallet)
            put("transferToWallet", tx.transferToWallet)
            put("isTransfer", if (tx.isTransfer) 1 else 0)
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
            put("transferToWallet", tx.transferToWallet)
            put("isTransfer", if (tx.isTransfer) 1 else 0)
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
                        transferToWallet = it.getString(it.getColumnIndexOrThrow("transferToWallet")) ?: "",
                        isTransfer = it.getInt(it.getColumnIndexOrThrow("isTransfer")) == 1,
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
        val sql = "SELECT type, SUM(amount) AS total FROM $TABLE_TRANSACTIONS WHERE date >= ? AND date <= ? AND isTransfer = 0 GROUP BY type"
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
            "SELECT date, type, SUM(amount) AS total FROM $TABLE_TRANSACTIONS WHERE date >= ? AND date <= ? AND isTransfer = 0 GROUP BY date(date/1000,'unixepoch','localtime'), type"
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
                        transferToWallet = it.getString(it.getColumnIndexOrThrow("transferToWallet")) ?: "",
                        isTransfer = it.getInt(it.getColumnIndexOrThrow("isTransfer")) == 1,
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
                    put("transferToWallet", tx.transferToWallet)
                    put("isTransfer", if (tx.isTransfer) 1 else 0)
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

    fun getAllWallets(): MutableList<WalletItem> {
        val cursor = readableDatabase.rawQuery(
            "SELECT name, balance FROM $TABLE_WALLETS ORDER BY name COLLATE NOCASE ASC",
            null
        )
        return cursor.use {
            val result = mutableListOf<WalletItem>()
            while (it.moveToNext()) {
                result += WalletItem(
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    balance = it.getLong(it.getColumnIndexOrThrow("balance"))
                )
            }
            result
        }
    }

    fun replaceWallets(items: List<WalletItem>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete(TABLE_WALLETS, null, null)
            items.forEach { wallet ->
                val values = ContentValues().apply {
                    put("name", wallet.name)
                    put("balance", wallet.balance)
                }
                writableDatabase.insert(TABLE_WALLETS, null, values)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun upsertWallet(item: WalletItem) {
        val values = ContentValues().apply {
            put("name", item.name)
            put("balance", item.balance)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_WALLETS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deleteWallet(name: String): Int {
        return writableDatabase.delete(TABLE_WALLETS, "name=?", arrayOf(name))
    }

    fun getAllNotes(): MutableList<NoteItem> {
        val cursor = readableDatabase.rawQuery(
            "SELECT id, title, contentHtml, updatedAt FROM $TABLE_NOTES ORDER BY updatedAt DESC",
            null
        )
        return cursor.use {
            val result = mutableListOf<NoteItem>()
            while (it.moveToNext()) {
                result += NoteItem(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    title = it.getString(it.getColumnIndexOrThrow("title")).orEmpty(),
                    contentHtml = it.getString(it.getColumnIndexOrThrow("contentHtml")).orEmpty(),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow("updatedAt"))
                )
            }
            result
        }
    }

    fun upsertNote(note: NoteItem) {
        val values = ContentValues().apply {
            put("id", note.id)
            put("title", note.title)
            put("contentHtml", note.contentHtml)
            put("updatedAt", note.updatedAt)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_NOTES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deleteNote(id: Long): Int {
        return writableDatabase.delete(TABLE_NOTES, "id=?", arrayOf(id.toString()))
    }

    companion object {
        private const val DB_NAME = "moneynote.db"
        private const val DB_VERSION = 4
        private const val TABLE_TRANSACTIONS = "transactions"
        private const val TABLE_WALLETS = "wallets"
        private const val TABLE_NOTES = "notes"

        @Volatile
        private var INSTANCE: MoneyNoteDatabase? = null

        fun getInstance(context: Context): MoneyNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MoneyNoteDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
