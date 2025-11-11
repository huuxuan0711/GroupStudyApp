package com.xmobile.project1groupstudyappnew.utils.ui.datetime

import android.os.Build
import android.widget.Button
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import com.zyyoona7.wheel.WheelView
import com.zyyoona7.wheel.adapter.ArrayWheelAdapter
import com.zyyoona7.wheel.listener.OnItemSelectedListener
import java.util.Locale

object DateTimePickerHelper {

    fun setupHourPicker(
        wheelHour: WheelView,
        wheelMinute: WheelView,
        btnHour: Button,
        onTimeSelected: (hour: Int, minute: Int) -> Unit
    ) {
        val hours = (0..23).map { String.format("%02d", it) }
        val minutes = (0..59).map { String.format("%02d", it) }

        wheelHour.setData(hours)
        wheelMinute.setData(minutes)

        val listener = object : OnItemSelectedListener {
            override fun onItemSelected(wheelView: WheelView, adapter: ArrayWheelAdapter<*>, position: Int) {
                val hour = wheelHour.getSelectedPosition()
                val minute = wheelMinute.getSelectedPosition()
                btnHour.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                onTimeSelected(hour, minute)
            }
        }

        wheelHour.setOnItemSelectedListener(listener)
        wheelMinute.setOnItemSelectedListener(listener)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setupDatePicker(
        datePicker: DatePicker,
        btnDate: Button,
        onDateSelected: (year: Int, month: Int, day: Int) -> Unit
    ) {
        datePicker.setOnDateChangedListener { _, year, monthOfYear, dayOfMonth ->
            btnDate.text = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, year)
            onDateSelected(year, monthOfYear + 1, dayOfMonth)
        }
    }
}