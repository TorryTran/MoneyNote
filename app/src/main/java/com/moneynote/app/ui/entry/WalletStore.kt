package com.moneynote.app.ui.entry

import android.content.Context
import com.moneynote.app.data.DataChangeTracker
import com.moneynote.app.data.MoneyNoteDatabase
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionType
import org.json.JSONArray
import org.json.JSONObject

class WalletStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val db = MoneyNoteDatabase.getInstance(context)
    private var legacyMigrated = false

    fun load(): MutableList<WalletItem> {
        migrateLegacyPrefsIfNeeded()
        val list = db.getAllWallets()
        if (list.isEmpty()) {
            val defaults = defaultWallets()
            db.replaceWallets(defaults)
            return defaults
        }
        return list
    }

    fun save(items: List<WalletItem>) {
        db.replaceWallets(items)
        DataChangeTracker.bumpWallets()
    }

    fun ensureWalletExists(name: String) {
        if (name.isBlank()) return
        val items = load()
        if (items.none { it.name == name }) {
            db.upsertWallet(WalletItem(name, 0L))
            DataChangeTracker.bumpWallets()
        }
    }

    fun removeWallet(name: String) {
        if (name.isBlank() || isProtectedWallet(name)) return
        db.deleteWallet(name)
        DataChangeTracker.bumpWallets()
    }

    fun isProtectedWallet(name: String): Boolean {
        return name == DEFAULT_CASH || name == DEFAULT_ACCOUNT
    }

    fun adjustBalance(name: String, delta: Long) {
        if (name.isBlank()) return
        val items = load()
        val index = items.indexOfFirst { it.name == name }
        if (index == -1) {
            db.upsertWallet(WalletItem(name, delta))
        } else {
            items[index].balance += delta
            db.upsertWallet(items[index])
        }
        DataChangeTracker.bumpWallets()
    }

    fun recalculateFromTransactions(transactions: List<TransactionEntity>) {
        val items = load()
        val map = linkedMapOf<String, Long>()
        items.forEach { map[it.name] = 0L }
        transactions.forEach { tx ->
            if (!map.containsKey(tx.wallet)) {
                map[tx.wallet] = 0L
            }
            if (tx.isTransfer) {
                map[tx.wallet] = (map[tx.wallet] ?: 0L) - tx.amount
                val target = tx.transferToWallet.trim()
                if (target.isNotEmpty()) {
                    if (!map.containsKey(target)) {
                        map[target] = 0L
                    }
                    map[target] = (map[target] ?: 0L) + tx.amount
                }
            } else {
                val delta = if (tx.type == TransactionType.INCOME) tx.amount else -tx.amount
                map[tx.wallet] = (map[tx.wallet] ?: 0L) + delta
            }
        }
        val next = map.map { WalletItem(it.key, it.value) }
        save(next)
    }

    private fun defaultWallets(): MutableList<WalletItem> {
        return mutableListOf(
            WalletItem(DEFAULT_CASH, 0L),
            WalletItem(DEFAULT_ACCOUNT, 0L)
        )
    }

    private fun migrateLegacyPrefsIfNeeded() {
        if (legacyMigrated) return
        legacyMigrated = true
        if (db.getAllWallets().isNotEmpty()) return

        val raw = prefs.getString(KEY_WALLETS, null)
        val migrated = if (raw.isNullOrBlank()) {
            defaultWallets()
        } else {
            try {
                val arr = JSONArray(raw)
                val list = mutableListOf<WalletItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list += WalletItem(
                        name = obj.getString("name"),
                        balance = obj.getLong("balance")
                    )
                }
                if (list.isEmpty()) defaultWallets() else list
            } catch (_: Exception) {
                defaultWallets()
            }
        }
        db.replaceWallets(migrated)
    }

    companion object {
        private const val PREFS = "wallets_prefs"
        private const val KEY_WALLETS = "wallets_json"
        private const val DEFAULT_CASH = "Tiền mặt"
        private const val DEFAULT_ACCOUNT = "Tài khoản"
    }
}
