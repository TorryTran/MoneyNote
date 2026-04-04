package com.moneynote.app.ui.entry

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.moneynote.app.R
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionRepository
import com.moneynote.app.data.TransactionType
import com.moneynote.app.databinding.DialogEditCategoriesBinding
import com.moneynote.app.databinding.DialogRenameCategoryBinding
import com.moneynote.app.databinding.FragmentEntryFormBinding
import com.moneynote.app.ui.common.AppCategories
import com.moneynote.app.ui.common.DateUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.NumberFormat
import java.util.Locale

class EntryFormFragment : Fragment() {
    private var _binding: FragmentEntryFormBinding? = null
    private val binding get() = _binding!!

    private lateinit var type: TransactionType
    private lateinit var repository: TransactionRepository
    private lateinit var walletStore: WalletStore
    private var selectedDate: Long = System.currentTimeMillis()
    private var isFormattingAmount = false
    private lateinit var categories: MutableList<com.moneynote.app.ui.common.CategoryItem>
    private var selectedCategoryName: String = ""
    private var selectedWalletName: String = "Tiền mặt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val raw = requireArguments().getString(ARG_TYPE) ?: TransactionType.EXPENSE.name
        type = TransactionType.valueOf(raw)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntryFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TransactionRepository.get(requireContext())
        walletStore = WalletStore(requireContext())
        setupWalletSpinner()

        categories = if (type == TransactionType.EXPENSE) AppCategories.expenseDefault() else AppCategories.incomeDefault()
        selectedCategoryName = categories.firstOrNull { !it.isEditor }?.name.orEmpty()
        lateinit var adapter: CategoryAdapter
        adapter = CategoryAdapter(categories) { item, position ->
            if (item.isEditor) {
                showEditCategoriesDialog(adapter)
                false
            } else {
                selectedCategoryName = item.name
                true
            }
        }
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvCategories.adapter = adapter
        binding.rvCategories.setHasFixedSize(true)

        binding.tvAmountLabel.text =
            if (type == TransactionType.EXPENSE) getString(R.string.label_amount_expense) else getString(R.string.label_amount_income)
        binding.btnSubmit.text =
            if (type == TransactionType.EXPENSE) getString(R.string.btn_add_expense) else getString(R.string.btn_add_income)

        renderDate()

        binding.btnPrevDate.setOnClickListener {
            selectedDate = DateUtils.shiftDay(selectedDate, -1)
            renderDate()
        }
        binding.btnNextDate.setOnClickListener {
            selectedDate = DateUtils.shiftDay(selectedDate, 1)
            renderDate()
        }
        binding.btnPickDate.setOnClickListener {
            pickDate()
        }

        binding.btnSubmit.setOnClickListener {
            val amountText = binding.etAmount.text?.toString()?.trim().orEmpty()
            val amount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
            if (amount <= 0) {
                Toast.makeText(requireContext(), getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedCategory = selectedCategoryName
            if (selectedCategory.isBlank()) {
                Toast.makeText(requireContext(), getString(R.string.choose_category), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedWalletName.isBlank()) {
                Toast.makeText(requireContext(), getString(R.string.wallet_missing), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val note = binding.etNote.text?.toString()?.trim().orEmpty()
            val transaction = TransactionEntity(
                type = type,
                amount = amount,
                wallet = selectedWalletName,
                category = selectedCategory,
                note = note,
                date = selectedDate
            )

            viewLifecycleOwner.lifecycleScope.launch {
                repository.add(transaction)
                val delta = if (type == TransactionType.INCOME) amount else -amount
                walletStore.adjustBalance(selectedWalletName, delta)
                val ui = _binding ?: return@launch
                Toast.makeText(requireContext(), getString(R.string.saved_success), Toast.LENGTH_SHORT).show()
                ui.etAmount.setText("")
                ui.tvAmountSuffix.visibility = View.GONE
                ui.etNote.setText("")
            }
        }

        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingAmount) return
                val raw = s?.toString().orEmpty().filter { it.isDigit() }
                if (raw.isBlank()) {
                    if (s?.isNotEmpty() == true) {
                        isFormattingAmount = true
                        binding.etAmount.setText("")
                        isFormattingAmount = false
                    }
                    binding.tvAmountSuffix.visibility = View.GONE
                    return
                }
                val parsed = raw.toLongOrNull() ?: return
                val formatted = NumberFormat.getInstance(Locale("vi", "VN")).format(parsed)
                binding.tvAmountSuffix.visibility = View.VISIBLE
                if (formatted == s.toString()) return
                isFormattingAmount = true
                binding.etAmount.setText(formatted)
                binding.etAmount.setSelection(formatted.length)
                isFormattingAmount = false
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            setupWalletSpinner()
        }
    }

    private fun setupWalletSpinner() {
        val wallets = walletStore.load()
        if (wallets.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.wallet_missing), Toast.LENGTH_SHORT).show()
            return
        }
        val walletNames = wallets.map { it.name }
        val walletAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_selected, walletNames).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spWallet.adapter = walletAdapter
        val selectedIndex = walletNames.indexOf(selectedWalletName).takeIf { it >= 0 } ?: 0
        selectedWalletName = walletNames[selectedIndex]
        binding.spWallet.setSelection(selectedIndex)
        binding.spWallet.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedWalletName = walletNames[position]
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun pickDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val dialog = DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                selectedDate = cal.timeInMillis
                renderDate()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun renderDate() {
        binding.tvDate.text = DateUtils.formatDate(selectedDate)
    }

    private fun showEditCategoriesDialog(adapter: CategoryAdapter) {
        val editable = categories.withIndex().filter { !it.value.isEditor }
        if (editable.isEmpty()) return

        val dialogBinding = DialogEditCategoriesBinding.inflate(layoutInflater)
        val manageAdapter = CategoryManageAdapter(editable) { pair ->
            showRenameDialog(pair.index, pair.value.name, adapter) {
                dialogBinding.rvCategoryManage.adapter?.notifyDataSetChanged()
            }
        }
        dialogBinding.rvCategoryManage.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        dialogBinding.rvCategoryManage.adapter = manageAdapter

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel)
    }

    private fun showRenameDialog(
        index: Int,
        oldName: String,
        adapter: CategoryAdapter,
        onUpdated: () -> Unit
    ) {
        val renameBinding = DialogRenameCategoryBinding.inflate(layoutInflater)
        renameBinding.etCategoryName.setText(oldName)
        renameBinding.etCategoryName.setSelection(oldName.length)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_rename_title))
            .setView(renameBinding.root)
            .setPositiveButton(getString(R.string.action_update)) { _, _ ->
                val newName = renameBinding.etCategoryName.text?.toString()?.trim().orEmpty()
                if (newName.isNotBlank()) {
                    categories[index].name = newName
                    if (selectedCategoryName == oldName) {
                        selectedCategoryName = newName
                    }
                    adapter.refreshItem(index)
                    onUpdated()
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
            .also { it.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TYPE = "arg_type"

        fun newInstance(type: TransactionType): EntryFormFragment {
            val fragment = EntryFormFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_TYPE, type.name)
            }
            return fragment
        }
    }
}
