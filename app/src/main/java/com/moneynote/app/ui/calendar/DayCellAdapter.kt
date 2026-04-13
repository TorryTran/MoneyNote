package com.moneynote.app.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moneynote.app.R
import com.moneynote.app.data.DaySummary
import com.moneynote.app.databinding.ItemDayCellBinding
import com.moneynote.app.ui.common.MoneyFormat

class DayCellAdapter(
    private val onDayClick: (Int) -> Unit
) : RecyclerView.Adapter<DayCellAdapter.DayCellVH>() {

    private var days: List<Int?> = emptyList()
    private var summaries: Map<Int, DaySummary> = emptyMap()
    private var selectedDay: Int? = null

    fun submit(days: List<Int?>, summaries: Map<Int, DaySummary>, selectedDay: Int?) {
        val oldDays = this.days
        val oldSummaries = this.summaries
        val oldSelectedDay = this.selectedDay
        this.days = days
        this.summaries = summaries
        this.selectedDay = selectedDay
        if (oldDays.size != days.size) {
            notifyDataSetChanged()
            return
        }
        for (index in days.indices) {
            val oldDay = oldDays[index]
            val newDay = days[index]
            val changed = oldDay != newDay ||
                oldSummaries[oldDay] != summaries[newDay] ||
                (oldSelectedDay == oldDay) != (selectedDay == newDay)
            if (changed) {
                notifyItemChanged(index)
            }
        }
    }

    fun selectDay(newSelectedDay: Int?) {
        val old = selectedDay
        if (old == newSelectedDay) return
        selectedDay = newSelectedDay
        old?.let {
            val index = days.indexOf(it)
            if (index >= 0) notifyItemChanged(index)
        }
        newSelectedDay?.let {
            val index = days.indexOf(it)
            if (index >= 0) notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayCellVH {
        val binding = ItemDayCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayCellVH(binding)
    }

    override fun getItemCount(): Int = days.size

    override fun onBindViewHolder(holder: DayCellVH, position: Int) {
        holder.bind(days[position])
    }

    inner class DayCellVH(private val binding: ItemDayCellBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(day: Int?) {
            if (day == null) {
                binding.tvDay.text = ""
                binding.tvIncome.text = ""
                binding.tvExpense.text = ""
                binding.root.alpha = 0.4f
                binding.root.setOnClickListener(null)
                binding.root.setBackgroundResource(R.drawable.bg_day_normal)
                return
            }

            binding.root.alpha = 1f
            binding.tvDay.text = day.toString()
            val summary = summaries[day]
            binding.tvIncome.text = if ((summary?.incomeTotal ?: 0L) > 0L) {
                MoneyFormat.format(summary?.incomeTotal ?: 0L)
            } else {
                ""
            }
            binding.tvExpense.text = if ((summary?.expenseTotal ?: 0L) > 0L) {
                MoneyFormat.format(summary?.expenseTotal ?: 0L)
            } else {
                ""
            }
            binding.root.setBackgroundResource(if (selectedDay == day) R.drawable.bg_day_selected else R.drawable.bg_day_normal)
            binding.root.setOnClickListener { onDayClick(day) }
        }
    }
}
