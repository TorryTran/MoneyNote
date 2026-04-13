package com.moneynote.app.ui.entry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.moneynote.app.R
import com.moneynote.app.databinding.ItemCategoryBinding
import com.moneynote.app.ui.common.CategoryItem
import com.moneynote.app.ui.common.parseColorOrDefault

class CategoryAdapter(
    private val categories: MutableList<CategoryItem>,
    private val onSelected: (CategoryItem, Int) -> Boolean
) : RecyclerView.Adapter<CategoryAdapter.CategoryVH>() {

    private var selectedIndex = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryVH(binding)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: CategoryVH, position: Int) {
        holder.bind(categories[position], position == selectedIndex)
        holder.itemView.setOnClickListener {
            val accept = onSelected(categories[position], position)
            if (!accept) return@setOnClickListener
            val old = selectedIndex
            selectedIndex = position
            notifyItemChanged(old)
            notifyItemChanged(selectedIndex)
        }
    }

    fun selectedCategory(): CategoryItem? = categories.getOrNull(selectedIndex)

    fun refreshItem(index: Int) {
        notifyItemChanged(index)
    }

    fun setSelectedByName(name: String) {
        if (categories.isEmpty()) return
        val idx = categories.indexOfFirst { !it.isEditor && it.name == name }
        val target = if (idx >= 0) idx else categories.indexOfFirst { !it.isEditor }.coerceAtLeast(0)
        if (target == selectedIndex) return
        val old = selectedIndex
        selectedIndex = target
        if (old in categories.indices) notifyItemChanged(old)
        if (selectedIndex in categories.indices) notifyItemChanged(selectedIndex)
    }

    inner class CategoryVH(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryItem, selected: Boolean) {
            binding.tvCategoryName.text = item.name
            val fallback = ContextCompat.getColor(itemView.context, R.color.accent_blue)
            val color = parseColorOrDefault(item.colorHex, fallback)
            binding.ivCategoryIcon.setImageResource(item.iconRes)
            binding.ivCategoryIcon.setColorFilter(color)
            binding.root.setBackgroundResource(if (selected) R.drawable.bg_category_selected else R.drawable.bg_category)
        }
    }
}
