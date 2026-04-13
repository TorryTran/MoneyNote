package com.moneynote.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.moneynote.app.R
import kotlin.math.min

data class PieSlice(
    val label: String,
    val value: Long,
    val color: Int
)

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.chart_empty_gray)
    }

    private val arcBounds = RectF()
    private var slices: List<PieSlice> = emptyList()
    private var centerLabel: String = ""

    fun setSlices(slices: List<PieSlice>) {
        this.slices = slices.filter { it.value > 0L }
        invalidate()
    }

    fun setCenterLabel(label: String) {
        centerLabel = label
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = slices.sumOf { it.value }
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val inset = 10f
        val diameter = size - inset * 2f
        val left = (width - diameter) / 2f
        val top = (height - diameter) / 2f
        arcBounds.set(left, top, left + diameter, top + diameter)

        if (total <= 0L) {
            canvas.drawArc(arcBounds, 0f, 360f, true, emptyPaint)
            return
        }

        var startAngle = -90f
        slices.forEach { slice ->
            val sweepAngle = 360f * (slice.value.toFloat() / total.toFloat())
            slicePaint.color = slice.color
            canvas.drawArc(arcBounds, startAngle, sweepAngle, true, slicePaint)
            startAngle += sweepAngle
        }
    }
}
