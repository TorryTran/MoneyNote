package com.moneynote.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.moneynote.app.R

class DualDailyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.income_green)
    }
    private val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.expense_red)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.divider_dark)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    private var income = LongArray(7)
    private var expense = LongArray(7)
    private val defaultLabels = arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    private var labels = defaultLabels.copyOf()

    fun setWeekValues(income: LongArray, expense: LongArray, labels: Array<String>) {
        this.income = income.copyOf(7)
        this.expense = expense.copyOf(7)
        this.labels = Array(7) { index -> labels.getOrNull(index) ?: defaultLabels[index] }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val availableTop = 8f
        val availableBottom = h - 28f
        val availableLeft = 6f
        val availableRight = w - 6f
        val chartHeight = availableBottom - availableTop
        val groupCount = 7
        val groupWidth = (availableRight - availableLeft) / groupCount
        val barWidth = groupWidth * 0.34f
        val gap = groupWidth * 0.08f
        val radius = 6f

        for (i in 0..4) {
            val y = availableTop + (chartHeight / 4f) * i
            canvas.drawLine(availableLeft, y, availableRight, y, gridPaint)
        }
        canvas.drawLine(availableLeft, availableBottom, availableRight, availableBottom, axisPaint)

        val maxValue = maxOf(
            income.maxOrNull() ?: 0L,
            expense.maxOrNull() ?: 0L,
            1L
        ).toFloat()

        for (i in 0 until groupCount) {
            val groupStart = availableLeft + i * groupWidth
            val incomeLeft = groupStart + (groupWidth - (barWidth * 2 + gap)) / 2f
            val expenseLeft = incomeLeft + barWidth + gap

            val incomeTop = availableBottom - chartHeight * (income[i] / maxValue)
            val expenseTop = availableBottom - chartHeight * (expense[i] / maxValue)

            canvas.drawRoundRect(
                RectF(incomeLeft, incomeTop, incomeLeft + barWidth, availableBottom),
                radius,
                radius,
                incomePaint
            )
            canvas.drawRoundRect(
                RectF(expenseLeft, expenseTop, expenseLeft + barWidth, availableBottom),
                radius,
                radius,
                expensePaint
            )

            val centerX = groupStart + groupWidth / 2f
            canvas.drawText(labels[i], centerX, h - 6f, labelPaint)
        }
    }
}
