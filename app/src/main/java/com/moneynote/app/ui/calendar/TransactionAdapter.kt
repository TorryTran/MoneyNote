package com.moneynote.app.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.moneynote.app.R
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionType
import com.moneynote.app.databinding.ItemTransactionBinding
import com.moneynote.app.ui.common.CategoryVisuals
import com.moneynote.app.ui.common.DateUtils
import com.moneynote.app.ui.common.MoneyFormat

class TransactionAdapter(
    private val onLongClick: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TxVH>() {

    private val items = mutableListOf<TransactionEntity>()

    fun submit(list: List<TransactionEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxVH {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TxVH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TxVH, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnLongClickListener {
            onLongClick(items[position])
            true
        }
    }

    inner class TxVH(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tx: TransactionEntity) {
            binding.tvCategory.text = tx.category
            val time = DateUtils.formatTime(tx.date)
            val meta = "[$time]  ${tx.wallet}"
            binding.tvNote.text = if (tx.note.isBlank()) meta else "$meta  •  ${tx.note}"
            binding.tvAmount.text = MoneyFormat.format(tx.amount)
            binding.ivTypeIcon.setImageResource(CategoryVisuals.iconResFor(tx.category))

            val amountColor = if (tx.type == TransactionType.INCOME) R.color.income_green else R.color.expense_red
            val dotColor = if (tx.type == TransactionType.INCOME) R.color.income_green else R.color.expense_red
            binding.tvAmount.setTextColor(ContextCompat.getColor(itemView.context, amountColor))
            binding.ivTypeIcon.setColorFilter(ContextCompat.getColor(itemView.context, dotColor))
        }
    }
}
