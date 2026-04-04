package com.moneynote.app.ui.calendar

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.moneynote.app.R
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionRepository
import com.moneynote.app.data.TransactionType
import com.moneynote.app.databinding.DialogEditTransactionBinding
import com.moneynote.app.databinding.FragmentCalendarBinding
import com.moneynote.app.ui.TabRefreshable
import com.moneynote.app.ui.common.DateUtils
import com.moneynote.app.ui.common.MoneyFormat
import com.moneynote.app.ui.common.vibrateWarning
import com.moneynote.app.ui.entry.WalletStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarFragment : Fragment(), TabRefreshable {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TransactionRepository
    private lateinit var walletStore: WalletStore
    private lateinit var dayAdapter: DayCellAdapter
    private lateinit var transactionAdapter: TransactionAdapter

    private var monthDate = System.currentTimeMillis()
    private var selectedDayDate = System.currentTimeMillis()
    private var loadDayJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TransactionRepository.get(requireContext())
        walletStore = WalletStore(requireContext())

        dayAdapter = DayCellAdapter { day -> onDaySelected(day) }
        binding.rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCalendar.adapter = dayAdapter
        binding.rvCalendar.setHasFixedSize(true)
        binding.rvCalendar.itemAnimator = null

        transactionAdapter = TransactionAdapter { tx ->
            showEditDeleteDialog(tx)
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = transactionAdapter
        binding.rvTransactions.setHasFixedSize(true)
        binding.rvTransactions.itemAnimator = null

        binding.btnPrevMonth.setOnClickListener {
            monthDate = DateUtils.shiftMonth(monthDate, -1)
            selectedDayDate = monthDate
            refreshMonthAndDay()
        }
        binding.btnNextMonth.setOnClickListener {
            monthDate = DateUtils.shiftMonth(monthDate, 1)
            selectedDayDate = monthDate
            refreshMonthAndDay()
        }
        binding.btnPickMonth.setOnClickListener { pickMonth() }

        refreshMonthAndDay()
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) refreshMonthAndDay()
    }

    override fun refreshTab() {
        if (_binding != null) refreshMonthAndDay()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) {
            refreshMonthAndDay()
        }
    }

    private fun refreshMonthAndDay() {
        val selectedDay = DateUtils.dayOfMonth(selectedDayDate)
        binding.tvMonth.text = DateUtils.formatMonth(monthDate)
        viewLifecycleOwner.lifecycleScope.launch {
            val uiData = loadMonthUiData(monthDate)
            val ui = _binding ?: return@launch
            dayAdapter.submit(uiData.grid, uiData.daySummaryMap, selectedDay)

            ui.tvIncomeSummary.text = getString(R.string.summary_income) + "\n" + MoneyFormat.format(requireContext(), uiData.incomeTotal)
            ui.tvExpenseSummary.text = getString(R.string.summary_expense) + "\n" + MoneyFormat.format(requireContext(), uiData.expenseTotal)
            ui.tvBalanceSummary.text = getString(R.string.summary_balance) + "\n" + MoneyFormat.format(requireContext(), uiData.incomeTotal - uiData.expenseTotal)

            renderSelectedDay()
        }
    }

    private fun onDaySelected(day: Int) {
        selectedDayDate = DateUtils.buildDayInMonth(monthDate, day)
        dayAdapter.selectDay(day)
        renderSelectedDay()
    }

    private fun renderSelectedDay() {
        val ui = _binding ?: return
        ui.tvSelectedDayLabel.text = DateUtils.formatDate(selectedDayDate)
        loadDayTransactions(selectedDayDate)
    }

    private fun loadDayTransactions(dayDate: Long) {
        val (start, end) = DateUtils.dayBounds(dayDate)
        loadDayJob?.cancel()
        loadDayJob = viewLifecycleOwner.lifecycleScope.launch {
            val list = repository.getByDay(start, end)
            val ui = _binding ?: return@launch
            transactionAdapter.submit(list)
            ui.tvEmpty.isVisible = list.isEmpty()
            if (list.isNotEmpty()) {
                ui.rvTransactions.post {
                    ui.rvTransactions.scrollToPosition(0)
                }
            }
        }
    }

    private suspend fun loadMonthUiData(date: Long): MonthUiData = coroutineScope {
        val (monthStart, monthEnd) = DateUtils.monthBounds(date)
        val daySummaryDeferred = async { repository.getMonthDaySummary(monthStart, monthEnd) }
        val summaryDeferred = async { repository.getMonthSummary(monthStart, monthEnd) }
        val gridDeferred = async(Dispatchers.Default) { DateUtils.monthGrid(date) }

        val daySummaryMap = daySummaryDeferred.await()
        val summary = summaryDeferred.await()
        val grid = gridDeferred.await()
        MonthUiData(grid, daySummaryMap, summary.incomeTotal, summary.expenseTotal)
    }

    private fun pickMonth() {
        val cal = Calendar.getInstance().apply { timeInMillis = monthDate }
        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, _ ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                monthDate = cal.timeInMillis
                selectedDayDate = monthDate
                refreshMonthAndDay()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun showEditDeleteDialog(tx: TransactionEntity) {
        val editBinding = DialogEditTransactionBinding.inflate(layoutInflater)
        val types = listOf(getString(R.string.tab_income), getString(R.string.tab_expense))
        val typeAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_selected, types).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        editBinding.spType.adapter = typeAdapter
        editBinding.spType.setSelection(if (tx.type == TransactionType.INCOME) 0 else 1)
        editBinding.etAmount.setText(tx.amount.toString())
        editBinding.etCategory.setText(tx.category)
        editBinding.etWallet.setText(tx.wallet)
        editBinding.etNote.setText(tx.note)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(editBinding.root)
            .setPositiveButton(getString(R.string.action_update)) { _, _ ->
                val amount = editBinding.etAmount.text?.toString()?.trim()?.toLongOrNull() ?: 0L
                if (amount <= 0L) return@setPositiveButton
                val updated = tx.copy(
                    type = if (editBinding.spType.selectedItemPosition == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                    amount = amount,
                    wallet = editBinding.etWallet.text?.toString()?.trim().orEmpty().ifBlank { tx.wallet },
                    category = editBinding.etCategory.text?.toString()?.trim().orEmpty().ifBlank { tx.category },
                    note = editBinding.etNote.text?.toString()?.trim().orEmpty()
                )
                val oldDelta = if (tx.type == TransactionType.INCOME) tx.amount else -tx.amount
                val newDelta = if (updated.type == TransactionType.INCOME) updated.amount else -updated.amount
                val balanceMap = walletStore.load().associate { it.name to it.balance }.toMutableMap()
                balanceMap[tx.wallet] = (balanceMap[tx.wallet] ?: 0L) - oldDelta
                balanceMap[updated.wallet] = (balanceMap[updated.wallet] ?: 0L) + newDelta
                if ((balanceMap[updated.wallet] ?: 0L) < 0L || (balanceMap[tx.wallet] ?: 0L) < 0L) {
                    vibrateWarning(requireContext())
                    Toast.makeText(requireContext(), getString(R.string.wallet_insufficient_balance), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.update(updated)
                    walletStore.adjustBalance(tx.wallet, -oldDelta)
                    walletStore.adjustBalance(updated.wallet, newDelta)
                    refreshMonthAndDay()
                }
            }
            .setNeutralButton(getString(R.string.action_delete)) { _, _ ->
                showDeleteDialog(tx)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel)
        styleActionButtons(dialog)
    }

    private fun showDeleteDialog(tx: TransactionEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_message))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.delete(tx.id)
                    val oldDelta = if (tx.type == TransactionType.INCOME) tx.amount else -tx.amount
                    walletStore.adjustBalance(tx.wallet, -oldDelta)
                    refreshMonthAndDay()
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
            .also { it.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel) }
    }

    private fun styleActionButtons(dialog: AlertDialog) {
        val accent = ContextCompat.getColor(requireContext(), R.color.accent_blue)
        val secondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            isAllCaps = false
            setTextColor(accent)
            textSize = 16f
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            isAllCaps = false
            setTextColor(secondary)
            textSize = 16f
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
            isAllCaps = false
            setTextColor(accent)
            textSize = 16f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadDayJob?.cancel()
        loadDayJob = null
        _binding = null
    }

    private data class MonthUiData(
        val grid: List<Int?>,
        val daySummaryMap: Map<Int, com.moneynote.app.data.DaySummary>,
        val incomeTotal: Long,
        val expenseTotal: Long
    )
}
