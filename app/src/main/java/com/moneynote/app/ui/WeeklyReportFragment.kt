package com.moneynote.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.moneynote.app.R
import com.moneynote.app.data.DataChangeTracker
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionRepository
import com.moneynote.app.data.TransactionType
import com.moneynote.app.databinding.FragmentReportWeeklyBinding
import com.moneynote.app.ui.common.DateUtils
import com.moneynote.app.ui.common.MoneyFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class WeeklyReportFragment : Fragment(), TabRefreshable {
    private var _binding: FragmentReportWeeklyBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TransactionRepository
    private var weekCursorDate = System.currentTimeMillis()
    private var refreshJob: Job? = null
    private var preloadJob: Job? = null
    private var latestRefreshToken = 0L
    private val weekDataCache = linkedMapOf<Long, WeekUiData>()
    private var lastTransactionsVersion = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportWeeklyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TransactionRepository.get(requireContext())
        binding.btnPrevWeek.setOnClickListener { shiftWeek(-1) }
        binding.btnNextWeek.setOnClickListener { shiftWeek(1) }
        refreshWeek(forceRefreshWeek = true)
    }

    override fun refreshTab() {
        if (_binding != null) {
            val currentVersion = DataChangeTracker.currentTransactionsVersion()
            if (currentVersion != lastTransactionsVersion) {
                refreshWeek(forceRefreshWeek = true)
            }
        }
    }

    private fun shiftWeek(direction: Int) {
        weekCursorDate = DateUtils.shiftDay(weekCursorDate, direction * 7)
        refreshWeek(forceRefreshWeek = false)
    }

    private fun refreshWeek(forceRefreshWeek: Boolean) {
        val (weekStart, weekEnd) = DateUtils.weekBounds(weekCursorDate)
        val refreshToken = ++latestRefreshToken
        refreshJob?.cancel()
        if (forceRefreshWeek) {
            weekDataCache.remove(weekStart)
        }

        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            val ui = _binding ?: return@launch
            weekDataCache[weekStart]?.let { cachedWeek ->
                bindWeekUi(ui, weekStart, weekEnd, cachedWeek)
            }
            val weekDeferred = async { getWeekData(weekStart, weekEnd, forceRefreshWeek) }
            val weekData = weekDeferred.await()
            if (!isActive || refreshToken != latestRefreshToken) return@launch
            val latestUi = _binding ?: return@launch
            bindWeekUi(latestUi, weekStart, weekEnd, weekData)
            lastTransactionsVersion = DataChangeTracker.currentTransactionsVersion()
            preloadWeeksInBackground(weekCursorDate, refreshToken)
        }
    }

    private fun bindWeekUi(ui: FragmentReportWeeklyBinding, weekStart: Long, weekEnd: Long, d: WeekUiData) {
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

    private suspend fun getWeekData(
        weekStart: Long,
        weekEnd: Long,
        forceRefresh: Boolean = false
    ): WeekUiData {
        val cached = if (forceRefresh) null else weekDataCache[weekStart]
        return cached ?: run {
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
            if (it.isTransfer) return@forEach
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
            if (it.type == TransactionType.INCOME) incomeByDay[weekIndex] += it.amount
            else expenseByDay[weekIndex] += it.amount
        }
        return WeekUiData(weekIncome, weekExpense, incomeByDay, expenseByDay)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshJob?.cancel()
        preloadJob?.cancel()
        refreshJob = null
        preloadJob = null
        _binding = null
    }

    private data class WeekUiData(
        val weekIncome: Long,
        val weekExpense: Long,
        val incomeByDay: LongArray,
        val expenseByDay: LongArray
    )
    companion object {
        private const val PRELOAD_RADIUS_WEEKS = 12
    }
}
