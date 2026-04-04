package com.moneynote.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.moneynote.app.R
import com.moneynote.app.data.MonthSummary
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionRepository
import com.moneynote.app.data.TransactionType
import com.moneynote.app.databinding.FragmentReportBinding
import com.moneynote.app.ui.common.DateUtils
import com.moneynote.app.ui.common.MoneyFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs

class ReportFragment : Fragment() {
    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TransactionRepository
    private var weekCursorDate = System.currentTimeMillis()
    private var refreshJob: Job? = null
    private var preloadJob: Job? = null
    private var latestRefreshToken = 0L
    private var monthSummaryCache: CachedMonthSummary? = null
    private val weekDataCache = linkedMapOf<Long, WeekUiData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TransactionRepository.get(requireContext())
        binding.btnPrevWeek.setOnClickListener { shiftWeek(-1) }
        binding.btnNextWeek.setOnClickListener { shiftWeek(1) }
    }

    override fun onResume() {
        super.onResume()
        if (weekCursorDate <= 0L) weekCursorDate = System.currentTimeMillis()
        refreshSummaryAndWeek(forceRefreshMonth = false)
    }

    private fun shiftWeek(direction: Int) {
        weekCursorDate = DateUtils.shiftDay(weekCursorDate, direction * 7)
        refreshSummaryAndWeek(forceRefreshMonth = false)
    }

    private fun refreshSummaryAndWeek(forceRefreshMonth: Boolean) {
        val now = System.currentTimeMillis()
        val (monthStart, monthEnd) = DateUtils.monthBounds(now)
        val (weekStart, weekEnd) = DateUtils.weekBounds(weekCursorDate)
        val refreshToken = ++latestRefreshToken
        refreshJob?.cancel()

        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            val ui = _binding ?: return@launch

            val cachedSummary = monthSummaryCache?.takeIf {
                it.monthStart == monthStart && it.monthEnd == monthEnd
            }?.summary
            if (cachedSummary != null) {
                bindMonthSummary(ui, now, cachedSummary)
            }
            bindWeekUi(ui, weekStart, weekEnd, weekDataCache[weekStart] ?: EMPTY_WEEK_DATA)

            val summaryDeferred = async(Dispatchers.IO) {
                val cache = monthSummaryCache
                if (!forceRefreshMonth &&
                    cache != null &&
                    cache.monthStart == monthStart &&
                    cache.monthEnd == monthEnd
                ) {
                    cache.summary
                } else {
                    repository.getMonthSummary(monthStart, monthEnd).also {
                        monthSummaryCache = CachedMonthSummary(monthStart, monthEnd, it)
                    }
                }
            }
            val weekDeferred = async {
                getWeekData(weekStart, weekEnd)
            }

            val summary = summaryDeferred.await()
            val weekData = weekDeferred.await()

            if (!isActive || refreshToken != latestRefreshToken) return@launch
            val uiLatest = _binding ?: return@launch
            bindMonthSummary(uiLatest, now, summary)
            bindWeekUi(uiLatest, weekStart, weekEnd, weekData)
            preloadWeeksInBackground(weekCursorDate, refreshToken)
        }
    }

    private fun bindMonthSummary(ui: FragmentReportBinding, now: Long, summary: MonthSummary) {
        ui.tvMonthLabel.text = getString(R.string.report_month_label, DateUtils.formatMonth(now))
        ui.tvIncome.text = getString(R.string.summary_income) + ": " + MoneyFormat.format(summary.incomeTotal)
        ui.tvExpense.text = getString(R.string.summary_expense) + ": " + MoneyFormat.format(summary.expenseTotal)
        ui.tvBalance.text = getString(R.string.summary_balance) + ": " + MoneyFormat.format(summary.balance)
    }

    private fun bindWeekUi(ui: FragmentReportBinding, weekStart: Long, weekEnd: Long, d: WeekUiData) {
        ui.tvTodayLabel.text = getString(
            R.string.report_week_label,
            DateUtils.formatDate(weekStart),
            DateUtils.formatDate(weekEnd)
        )
        ui.tvTodayIncome.text = getString(R.string.report_week_income, MoneyFormat.format(d.weekIncome))
        ui.tvTodayExpense.text = getString(R.string.report_week_expense, MoneyFormat.format(d.weekExpense))
        ui.chartDaily.setWeekValues(
            d.incomeByDay,
            d.expenseByDay,
            arrayOf(
                getString(R.string.week_short_mon),
                getString(R.string.week_short_tue),
                getString(R.string.week_short_wed),
                getString(R.string.week_short_thu),
                getString(R.string.week_short_fri),
                getString(R.string.week_short_sat),
                getString(R.string.week_short_sun)
            )
        )
        when {
            d.weekIncome > d.weekExpense -> {
                ui.tvTodayStatus.text = getString(R.string.report_week_status_positive)
                ui.tvTodayStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green))
            }
            d.weekIncome < d.weekExpense -> {
                ui.tvTodayStatus.text = getString(R.string.report_week_status_negative)
                ui.tvTodayStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
            }
            else -> {
                ui.tvTodayStatus.text = getString(R.string.report_week_status_balanced)
                ui.tvTodayStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue))
            }
        }
    }

    private fun preloadWeeksInBackground(centerDate: Long, refreshToken: Long) {
        preloadJob?.cancel()
        preloadJob = viewLifecycleOwner.lifecycleScope.launch {
            for (offset in -PRELOAD_RADIUS_WEEKS..PRELOAD_RADIUS_WEEKS) {
                if (offset == 0) continue
                if (!isActive || refreshToken != latestRefreshToken) return@launch
                val date = DateUtils.shiftDay(centerDate, offset * 7)
                val (start, end) = DateUtils.weekBounds(date)
                if (!weekDataCache.containsKey(start)) {
                    getWeekData(start, end)
                }
            }
        }
    }

    private suspend fun getWeekData(weekStart: Long, weekEnd: Long): WeekUiData {
        return weekDataCache[weekStart] ?: run {
            val weekList = withContext(Dispatchers.IO) { repository.getByDay(weekStart, weekEnd) }
            withContext(Dispatchers.Default) { buildWeekUiData(weekList) }.also {
                weekDataCache[weekStart] = it
                if (weekDataCache.size > 24) {
                    val firstKey = weekDataCache.keys.firstOrNull()
                    if (firstKey != null) weekDataCache.remove(firstKey)
                }
            }
        }
    }

    private fun buildWeekUiData(weekList: List<TransactionEntity>): WeekUiData {
        var weekIncome = 0L
        var weekExpense = 0L
        val incomeByDay = LongArray(7)
        val expenseByDay = LongArray(7)
        val cal = Calendar.getInstance()
        weekList.forEach {
            if (it.type == TransactionType.INCOME) weekIncome += it.amount else weekExpense += it.amount
            cal.timeInMillis = it.date
            val weekIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                else -> 6
            }
            if (it.type == TransactionType.INCOME) {
                incomeByDay[weekIndex] += it.amount
            } else {
                expenseByDay[weekIndex] += it.amount
            }
        }
        return WeekUiData(weekIncome, weekExpense, incomeByDay, expenseByDay)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshJob?.cancel()
        refreshJob = null
        preloadJob?.cancel()
        preloadJob = null
        _binding = null
    }

    private data class CachedMonthSummary(
        val monthStart: Long,
        val monthEnd: Long,
        val summary: MonthSummary
    )

    private data class WeekUiData(
        val weekIncome: Long,
        val weekExpense: Long,
        val incomeByDay: LongArray,
        val expenseByDay: LongArray
    )

    companion object {
        private const val PRELOAD_RADIUS_WEEKS = 12
        private val EMPTY_WEEK_DATA = WeekUiData(0L, 0L, LongArray(7), LongArray(7))
    }
}
