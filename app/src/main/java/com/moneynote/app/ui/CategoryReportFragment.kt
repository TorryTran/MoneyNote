package com.moneynote.app.ui

import android.os.Bundle
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.moneynote.app.R
import com.moneynote.app.data.DataChangeTracker
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionRepository
import com.moneynote.app.data.TransactionType
import com.moneynote.app.databinding.FragmentReportCategoryBinding
import com.moneynote.app.ui.common.DateUtils
import com.moneynote.app.ui.common.MoneyFormat
import com.moneynote.app.ui.common.PieSlice
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class CategoryReportFragment : Fragment(), TabRefreshable {
    private var _binding: FragmentReportCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TransactionRepository
    private var refreshJob: Job? = null
    private var weekCursorDate = System.currentTimeMillis()
    private var lastTransactionsVersion = -1L
    private val chartPalette by lazy {
        intArrayOf(
            ContextCompat.getColor(requireContext(), R.color.expense_red),
            ContextCompat.getColor(requireContext(), R.color.income_green),
            ContextCompat.getColor(requireContext(), R.color.accent_blue),
            ContextCompat.getColor(requireContext(), R.color.accent_purple),
            0xFFFFB347.toInt(),
            0xFF5CD6D6.toInt()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TransactionRepository.get(requireContext())
        binding.btnPrevCategoryWeek.setOnClickListener { shiftWeek(-1) }
        binding.btnNextCategoryWeek.setOnClickListener { shiftWeek(1) }
        refreshTab()
    }

    override fun refreshTab() {
        if (_binding == null) return
        val currentVersion = DataChangeTracker.currentTransactionsVersion()
        if (currentVersion == lastTransactionsVersion) return
        refreshJob?.cancel()
        val (weekStart, weekEnd) = DateUtils.weekBounds(weekCursorDate)
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            val weekTransactions = repository.getByDay(weekStart, weekEnd)
            val ui = _binding ?: return@launch
            bindCategoryCharts(ui, weekStart, weekEnd, weekTransactions)
            lastTransactionsVersion = currentVersion
        }
    }

    private fun shiftWeek(direction: Int) {
        weekCursorDate = DateUtils.shiftDay(weekCursorDate, direction * 7)
        lastTransactionsVersion = -1L
        refreshTab()
    }

    private fun bindCategoryCharts(
        ui: FragmentReportCategoryBinding,
        weekStart: Long,
        weekEnd: Long,
        weekTransactions: List<TransactionEntity>
    ) {
        ui.tvCategoryPeriod.text = getString(
            R.string.report_category_period,
            DateUtils.formatDate(weekStart),
            DateUtils.formatDate(weekEnd)
        )
        ui.chartExpenseCategory.setCenterLabel("")
        ui.chartIncomeCategory.setCenterLabel("")
        ui.tvExpenseChartLabel.text = getString(R.string.report_expense_pie_title)
        ui.tvIncomeChartLabel.text = getString(R.string.report_income_pie_title)

        val expenseChart = buildCategoryChartData(weekTransactions, TransactionType.EXPENSE)
        val incomeChart = buildCategoryChartData(weekTransactions, TransactionType.INCOME)

        ui.chartExpenseCategory.setSlices(expenseChart.slices)
        ui.chartIncomeCategory.setSlices(incomeChart.slices)
        bindLegend(ui.layoutExpenseLegend, expenseChart)
        bindLegend(ui.layoutIncomeLegend, incomeChart)
    }

    private fun buildCategoryChartData(
        monthTransactions: List<TransactionEntity>,
        type: TransactionType
    ): CategoryChartData {
        val grouped = monthTransactions
            .asSequence()
            .filter { !it.isTransfer }
            .filter { it.type == type }
            .groupBy { it.category.trim().ifBlank { getString(R.string.category_other) } }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        if (grouped.isEmpty()) {
            return CategoryChartData(
                slices = emptyList(),
                legendRows = listOf(
                    LegendRow(
                        text = getString(R.string.report_category_legend_empty),
                        color = ContextCompat.getColor(requireContext(), R.color.chart_empty_gray)
                    )
                )
            )
        }

        val total = grouped.sumOf { it.second }.coerceAtLeast(1L)
        val primary = grouped.take(5).toMutableList()
        val otherTotal = grouped.drop(5).sumOf { it.second }
        if (otherTotal > 0L) {
            primary += getString(R.string.report_category_other) to otherTotal
        }

        val slices = primary.mapIndexed { index, (label, value) ->
            PieSlice(
                label = label,
                value = value,
                color = chartPalette[index % chartPalette.size]
            )
        }
        val legendRows = primary.mapIndexed { index, (label, value) ->
            val percent = value * 100f / total.toFloat()
            LegendRow(
                text = "${index + 1}. $label: ${String.format(Locale.US, "%.1f", percent)}%\n(${MoneyFormat.format(value)})",
                color = slices[index].color
            )
        }

        return CategoryChartData(slices, legendRows)
    }

    private fun bindLegend(container: LinearLayout, chartData: CategoryChartData) {
        container.removeAllViews()
        chartData.legendRows.forEach { row ->
            val textView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setTextColor(row.color)
                textSize = 15f
                text = row.text
                setLineSpacing(0f, 1.08f)
            }
            container.addView(textView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshJob?.cancel()
        refreshJob = null
        _binding = null
    }

    private data class CategoryChartData(
        val slices: List<PieSlice>,
        val legendRows: List<LegendRow>
    )

    private data class LegendRow(
        val text: String,
        val color: Int
    )
}
