package com.moneynote.app.ui.entry

import android.content.Context
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionType
import org.json.JSONArray
import org.json.JSONObject

class WalletStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): MutableList<WalletItem> {
        val raw = prefs.getString(KEY_WALLETS, null) ?: return defaultWallets()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<WalletItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WalletItem(
                        name = obj.getString("name"),
                        balance = obj.getLong("balance")
                    )
                )
            }
            if (list.isEmpty()) defaultWallets() else list
        } catch (_: Exception) {
            defaultWallets()
        }
    }

    fun save(items: List<WalletItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject().apply {
                    put("name", it.name)
                    put("balance", it.balance)
                }
            )
        }
        prefs.edit().putString(KEY_WALLETS, arr.toString()).apply()
    }

    fun ensureWalletExists(name: String) {
        if (name.isBlank()) return
        val items = load()
        if (items.none { it.name == name }) {
            items.add(WalletItem(name, 0L))
            save(items)
        }
    }

    fun removeWallet(name: String) {
        if (name.isBlank() || isProtectedWallet(name)) return
        val items = load()
        val next = items.filterNot { it.name == name }
        save(next)
    }

    fun isProtectedWallet(name: String): Boolean {
        return name == DEFAULT_CASH || name == DEFAULT_ACCOUNT
    }

    fun adjustBalance(name: String, delta: Long) {
        if (name.isBlank()) return
        val items = load()
        val index = items.indexOfFirst { it.name == name }
        if (index == -1) {
            items.add(WalletItem(name, delta))
        } else {
            items[index].balance += delta
        }
        save(items)
    }

    fun recalculateFromTransactions(transactions: List<TransactionEntity>) {
        val items = load()
        val map = linkedMapOf<String, Long>()
        items.forEach { map[it.name] = 0L }
        transactions.forEach { tx ->
            if (!map.containsKey(tx.wallet)) {
                map[tx.wallet] = 0L
            }
            val delta = if (tx.type == TransactionType.INCOME) tx.amount else -tx.amount
            map[tx.wallet] = (map[tx.wallet] ?: 0L) + delta
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

    companion object {
        private const val PREFS = "wallets_prefs"
        private const val KEY_WALLETS = "wallets_json"
        private const val DEFAULT_CASH = "Tiền mặt"
        private const val DEFAULT_ACCOUNT = "Tài khoản"
    }
}
