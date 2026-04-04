package com.moneynote.app.ui.entry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moneynote.app.R
import com.moneynote.app.databinding.ItemWalletBinding
import com.moneynote.app.ui.common.MoneyFormat

class WalletAdapter(
    private val items: MutableList<WalletItem>,
    private val onWalletClick: (WalletItem, Int) -> Unit,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<WalletAdapter.WalletVH>() {

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size) VIEW_TYPE_ADD else VIEW_TYPE_WALLET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletVH {
        val binding = ItemWalletBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalletVH(binding)
    }

    override fun getItemCount(): Int = items.size + 1

    override fun onBindViewHolder(holder: WalletVH, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_ADD) {
            holder.bindAdd(onAddClick)
        } else {
            holder.bindWallet(items[position]) { onWalletClick(items[position], position) }
        }
    }

    inner class WalletVH(private val binding: ItemWalletBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindWallet(item: WalletItem, onClick: () -> Unit) {
            binding.tvWalletName.text = item.name
            binding.tvWalletBalance.text = MoneyFormat.format(item.balance)
            binding.ivAdd.visibility = android.view.View.GONE
            binding.root.setOnClickListener { onClick() }
        }

        fun bindAdd(onClick: () -> Unit) {
            binding.tvWalletName.text = itemView.context.getString(R.string.wallet_add_new)
            binding.tvWalletBalance.text = itemView.context.getString(R.string.wallet_add_new_sub)
            binding.ivAdd.visibility = android.view.View.VISIBLE
            binding.root.setOnClickListener { onClick() }
        }
    }

    companion object {
        const val VIEW_TYPE_WALLET = 0
        const val VIEW_TYPE_ADD = 1
    }
}
