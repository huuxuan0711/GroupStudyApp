package com.xmobile.project1groupstudyappnew.utils.ui.chart

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.xmobile.project1groupstudyappnew.R

object Chart {
     fun setupPieChart(chart: PieChart, entries: List<PieEntry>, colors: List<Int>) {
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 12f
            sliceSpace = 2f
        }

        chart.apply {
            data = PieData(dataSet).apply { setValueFormatter(PercentFormatter()) }
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setUsePercentValues(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            legend.isEnabled = false
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    fun setupBarChart(context: Context, chart: BarChart, entries: List<BarEntry>, labels: List<String>) {
        val dataSet = BarDataSet(entries, context.getString(R.string.complete)).apply {
            color = ContextCompat.getColor(context, R.color.green)
            valueTextSize = 12f
            valueFormatter = PercentFormatter()
        }

        chart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            setFitBars(true)
            setScaleEnabled(false)
            setTouchEnabled(false)
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                textSize = 11f
                labelCount = labels.size
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 20f
                setDrawGridLines(true)
            }

            axisRight.isEnabled = false
            extraLeftOffset = 48f
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }
}