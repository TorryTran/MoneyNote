package com.moneynote.app.ui

import android.graphics.Rect
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.moneynote.app.R
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionRepository
import com.moneynote.app.data.TransactionType
import com.moneynote.app.databinding.FragmentWalletBinding
import com.moneynote.app.ui.entry.WalletAdapter
import com.moneynote.app.ui.entry.WalletItem
import com.moneynote.app.ui.entry.WalletStore
import kotlinx.coroutines.launch

class WalletFragment : Fragment(), TabRefreshable {
    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private lateinit var walletStore: WalletStore
    private lateinit var repository: TransactionRepository
    private lateinit var walletAdapter: WalletAdapter
    private val wallets: MutableList<WalletItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        walletStore = WalletStore(requireContext())
        repository = TransactionRepository.get(requireContext())

        walletAdapter = WalletAdapter(
            wallets,
            onWalletClick = { item, index -> showWalletActions(item, index) },
            onAddClick = { showAddWalletDialog() }
        )
        val gridLayoutManager = GridLayoutManager(requireContext(), 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (walletAdapter.getItemViewType(position) == WalletAdapter.VIEW_TYPE_ADD) 2 else 1
                }
            }
        }
        binding.rvWallets.layoutManager = gridLayoutManager
        binding.rvWallets.adapter = walletAdapter
        binding.rvWallets.addItemDecoration(SpacingDecoration(10.dp(), 10.dp()))
        binding.rvWallets.setHasFixedSize(true)
        binding.rvWallets.itemAnimator = null

        reloadWallets()
    }

    override fun onResume() {
        super.onResume()
        reloadWallets()
    }

    override fun refreshTab() {
        if (_binding != null) reloadWallets()
    }

    private fun reloadWallets() {
        wallets.clear()
        wallets.addAll(walletStore.load().sortedWith(walletComparator()))
        if (::walletAdapter.isInitialized) walletAdapter.notifyDataSetChanged()
    }

    private fun walletComparator(): Comparator<WalletItem> {
        return compareBy<WalletItem> {
            when (it.name) {
                getString(R.string.wallet_default_cash) -> 0
                getString(R.string.wallet_default_account) -> 1
                else -> 2
            }
        }.thenBy { it.name.lowercase() }
    }

    private fun Int.dp(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun showAddWalletDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.wallet_dialog_name_hint)
            background = resources.getDrawable(R.drawable.bg_dialog_input, null)
            setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        }
        val inputWrap = buildDialogInputContainer(input)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.wallet_dialog_add_title))
            .setView(inputWrap)
            .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isBlank()) return@setPositiveButton
                walletStore.ensureWalletExists(name)
                reloadWallets()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
            .also { it.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel) }
    }

    private fun showWalletActions(item: WalletItem, index: Int) {
        val options = if (walletStore.isProtectedWallet(item.name)) {
            arrayOf(getString(R.string.action_update))
        } else {
            arrayOf(getString(R.string.action_update), getString(R.string.action_delete))
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    getString(R.string.action_update) -> showEditWalletDialog(item, index)
                    getString(R.string.action_delete) -> showDeleteWalletDialog(item)
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
            .also { it.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel) }
    }

    private fun showEditWalletDialog(item: WalletItem, index: Int) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.wallet_dialog_balance_hint)
            setText(item.balance.toString())
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            background = resources.getDrawable(R.drawable.bg_dialog_input, null)
            setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
        }
        val inputWrap = buildDialogInputContainer(input)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.name)
            .setView(inputWrap)
            .setPositiveButton(getString(R.string.action_update)) { _, _ ->
                val newBalance = input.text?.toString()?.trim()?.toLongOrNull() ?: 0L
                val oldBalance = item.balance
                wallets[index].balance = newBalance
                walletStore.save(wallets)
                walletAdapter.notifyItemChanged(index)
                val delta = newBalance - oldBalance
                if (delta != 0L) {
                    val walletUpdateLabel = getString(R.string.category_wallet_update)
                    val logTx = TransactionEntity(
                        type = if (delta > 0L) TransactionType.INCOME else TransactionType.EXPENSE,
                        amount = kotlin.math.abs(delta),
                        wallet = item.name,
                        category = walletUpdateLabel,
                        note = walletUpdateLabel,
                        date = System.currentTimeMillis()
                    )
                    viewLifecycleOwner.lifecycleScope.launch {
                        repository.add(logTx)
                    }
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
            .also { it.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel) }
    }

    private fun buildDialogInputContainer(input: EditText): View {
        return FrameLayout(requireContext()).apply {
            setPadding(14.dp(), 8.dp(), 14.dp(), 2.dp())
            addView(
                input,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun showDeleteWalletDialog(item: WalletItem) {
        if (walletStore.isProtectedWallet(item.name)) {
            Toast.makeText(requireContext(), getString(R.string.wallet_protected_delete), Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.wallet_delete_title))
            .setMessage(getString(R.string.wallet_delete_message))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                walletStore.removeWallet(item.name)
                reloadWallets()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
            .also { it.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class SpacingDecoration(
        private val horizontal: Int,
        private val vertical: Int
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return

            outRect.left = horizontal / 2
            outRect.right = horizontal / 2
            outRect.top = if (position < 2) 0 else vertical
            outRect.bottom = 0
        }
    }
}
