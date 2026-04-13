package com.moneynote.app.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
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
) : ListAdapter<TransactionEntity, TransactionAdapter.TxVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxVH {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TxVH(binding)
    }

    fun submit(list: List<TransactionEntity>) {
        submitList(list.toList())
    }

    override fun onBindViewHolder(holder: TxVH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    inner class TxVH(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tx: TransactionEntity) {
            binding.tvCategory.text = tx.category
            val time = DateUtils.formatTime(tx.date)
            val meta = if (tx.isTransfer) {
                "[$time]  ${tx.wallet} → ${tx.transferToWallet}"
            } else {
                "[$time]  ${tx.wallet}"
            }
            binding.tvNote.text = if (tx.note.isBlank()) meta else "$meta  •  ${tx.note}"
            binding.tvAmount.text = MoneyFormat.format(tx.amount)
            binding.ivTypeIcon.setImageResource(CategoryVisuals.iconResFor(tx.category))

            val amountColor = when {
                tx.isTransfer -> R.color.accent_purple
                tx.type == TransactionType.INCOME -> R.color.income_green
                else -> R.color.expense_red
            }
            val dotColor = amountColor
            binding.tvAmount.setTextColor(ContextCompat.getColor(itemView.context, amountColor))
            binding.ivTypeIcon.setColorFilter(ContextCompat.getColor(itemView.context, dotColor))
            binding.tvCategory.setTextColor(ContextCompat.getColor(itemView.context, amountColor))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TransactionEntity>() {
            override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
