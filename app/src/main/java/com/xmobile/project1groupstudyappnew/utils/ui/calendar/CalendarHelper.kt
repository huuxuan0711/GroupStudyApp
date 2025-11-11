package com.xmobile.project1groupstudyappnew.utils.ui.calendar

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.viewmodel.TaskViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@RequiresApi(Build.VERSION_CODES.O)
class CalendarHelper(
    private val calendarView: com.kizitonwose.calendar.view.CalendarView,
    private val context: Context,
    private val taskViewModel: TaskViewModel,
    private val getSelectedDate: () -> LocalDate?,
    private val setSelectedDate: (LocalDate) -> Unit,
    private val getCurrentMonth: () -> YearMonth,
    private val onDateSelected: (String) -> Unit,
    private val getFormattedDate: (LocalDate) -> String
) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun setupCalendar(today: LocalDate) {
        initializeCalendar(today)
        onDateSelected(getFormattedDate(today))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initializeCalendar(today: LocalDate) {
        calendarView.setup(getCurrentMonth(), getCurrentMonth(), DayOfWeek.SUNDAY)
        calendarView.scrollToMonth(getCurrentMonth())

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                bindDay(container, data, today)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun bindDay(container: DayViewContainer, data: CalendarDay, today: LocalDate) {
        val txtDay = container.txtDay
        val layoutCountTask = container.layoutCountTask
        val txtCountTask = container.txtCountTask
        val formattedDate = getFormattedDate(data.date)

        txtDay.text = data.date.dayOfMonth.toString()

        // Style today / selected / other month
        container.view.setBackgroundResource(
            when {
                data.date == today -> R.drawable.bg_today
                data.date == getSelectedDate() -> R.drawable.bg_selected_day
                data.position != DayPosition.MonthDate -> R.drawable.bg_unselected_day
                else -> R.drawable.bg_unselected_day
            }
        )

        // Text color cho ngày ngoài tháng
        if (data.position != DayPosition.MonthDate) {
            val isDark = (context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            txtDay.setTextColor(context.getColor(if (isDark) R.color.black else R.color.lightGray))
        }

        // Badge số task
        val count = taskViewModel.countByDay.value[formattedDate] ?: 0
        layoutCountTask.visibility = if (count > 0) View.VISIBLE else View.GONE
        txtCountTask.text = count.toString()

        // Chọn ngày để load task
        container.view.setOnClickListener {
            if (data.position == DayPosition.MonthDate) {
                onDateSelected(formattedDate)
                setSelectedDate(data.date)
                calendarView.notifyCalendarChanged()
            }
        }
    }

    inner class DayViewContainer(view: View): ViewContainer(view) {
        val txtDay: TextView = view.findViewById(R.id.dayText)
        val layoutCountTask: ViewGroup = view.findViewById(R.id.layout_count_task)
        val txtCountTask: TextView = view.findViewById(R.id.countTask)
    }
}
