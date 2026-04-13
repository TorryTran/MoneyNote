package com.moneynote.app.ui.entry

import android.content.Context
import com.moneynote.app.data.DataChangeTracker
import com.moneynote.app.data.TransactionType
import com.moneynote.app.ui.common.AppCategories
import com.moneynote.app.ui.common.CategoryItem
import org.json.JSONArray
import org.json.JSONObject

class CategoryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var expenseCache: MutableList<CategoryItem>? = null
    private var incomeCache: MutableList<CategoryItem>? = null
    private val legacyExpenseNames = setOf(
        "Ăn uống", "Chi tiêu hàng ngày", "Quần áo", "Mỹ phẩm", "Phí giao lưu",
        "Y tế", "Giáo dục", "Tiền điện", "Đi lại", "Phí liên lạc", "Tiền nhà"
    )
    private val legacyIncomeNames = setOf(
        "Tiền lương", "Tiền phụ", "Tiền thưởng", "Thu nhập khác", "Đầu tư"
    )

    fun load(type: TransactionType): MutableList<CategoryItem> {
        cached(type)?.let { return cloneItems(it) }
        val fallback = defaultItems(type)
        val raw = prefs.getString(keyFor(type), null)
        val resolved = if (raw == null) {
            fallback
        } else try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CategoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CategoryItem(
                        name = obj.getString("name"),
                        iconRes = obj.getInt("iconRes"),
                        colorHex = obj.optString("colorHex", "#FFFFFF"),
                        isEditor = false
                    )
                )
            }
            if (list.isEmpty()) {
                fallback
            } else {
                normalizeLoaded(type, list, fallback)
            }
        } catch (_: Exception) {
            fallback
        }
        setCache(type, resolved)
        return cloneItems(resolved)
    }

    fun save(type: TransactionType, items: List<CategoryItem>) {
        val persistedItems = items.filterNot { it.isEditor }
        val arr = JSONArray()
        persistedItems.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("name", item.name)
                    put("iconRes", item.iconRes)
                    put("colorHex", item.colorHex)
                }
            )
        }
        prefs.edit().putString(keyFor(type), arr.toString()).apply()
        setCache(type, withEditor(persistedItems))
        DataChangeTracker.bumpCategories()
    }

    private fun defaultItems(type: TransactionType): MutableList<CategoryItem> {
        val defaults = if (type == TransactionType.EXPENSE) {
            AppCategories.expenseDefault()
        } else {
            AppCategories.incomeDefault()
        }
        return defaults.toMutableList()
    }

    private fun withEditor(items: List<CategoryItem>): MutableList<CategoryItem> {
        val list = items.map { it.copy(isEditor = false) }.toMutableList()
        val editor = AppCategories.expenseDefault().firstOrNull { it.isEditor }
        if (editor != null) {
            list.add(editor.copy())
        }
        return list
    }

    private fun keyFor(type: TransactionType): String {
        return if (type == TransactionType.EXPENSE) KEY_EXPENSE else KEY_INCOME
    }

    private fun normalizeLoaded(
        type: TransactionType,
        items: List<CategoryItem>,
        fallback: MutableList<CategoryItem>
    ): MutableList<CategoryItem> {
        val names = items.map { it.name }.toSet()
        val legacyNames = if (type == TransactionType.EXPENSE) legacyExpenseNames else legacyIncomeNames
        return if (names == legacyNames) fallback else withEditor(items)
    }

    private fun cached(type: TransactionType): MutableList<CategoryItem>? {
        return if (type == TransactionType.EXPENSE) expenseCache else incomeCache
    }

    private fun setCache(type: TransactionType, items: List<CategoryItem>) {
        val cloned = cloneItems(items)
        if (type == TransactionType.EXPENSE) {
            expenseCache = cloned
        } else {
            incomeCache = cloned
        }
    }

    private fun cloneItems(items: List<CategoryItem>): MutableList<CategoryItem> {
        return items.map { it.copy() }.toMutableList()
    }

    companion object {
        private const val PREFS = "categories_prefs"
        private const val KEY_EXPENSE = "expense_categories_json"
        private const val KEY_INCOME = "income_categories_json"
    }
}
