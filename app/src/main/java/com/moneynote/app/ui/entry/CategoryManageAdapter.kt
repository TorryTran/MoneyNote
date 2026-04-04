package com.moneynote.app.ui.entry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.moneynote.app.R
import com.moneynote.app.databinding.ItemManageCategoryBinding
import com.moneynote.app.ui.common.CategoryItem
import com.moneynote.app.ui.common.parseColorOrDefault

class CategoryManageAdapter(
    private val items: List<IndexedValue<CategoryItem>>,
    private val onClick: (IndexedValue<CategoryItem>) -> Unit
) : RecyclerView.Adapter<CategoryManageAdapter.ManageVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageVH {
        val binding = ItemManageCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ManageVH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ManageVH, position: Int) {
        holder.bind(items[position])
    }

    inner class ManageVH(private val binding: ItemManageCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IndexedValue<CategoryItem>) {
            binding.tvName.text = item.value.name
            val fallback = ContextCompat.getColor(itemView.context, R.color.accent_blue)
            val color = parseColorOrDefault(item.value.colorHex, fallback)
            binding.ivIcon.setImageResource(item.value.iconRes)
            binding.ivIcon.setColorFilter(color)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
