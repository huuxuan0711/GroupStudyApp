package com.xmobile.project1groupstudyappnew.view.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.FragmentCalendarBinding
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.state.TaskUIState
import com.xmobile.project1groupstudyappnew.utils.ui.calendar.CalendarHelper
import com.xmobile.project1groupstudyappnew.view.activity.TaskDetailActivity
import com.xmobile.project1groupstudyappnew.view.adapter.DeadlineAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class CalendarFragment : BaseFragment() {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var group: Group? = null
    private var inGroup = false
    private lateinit var userId: String
    private lateinit var currentMonth: YearMonth

    private var currentFullTaskList: List<Task> = emptyList()
    private var currentDate: String? = null
    private var selectedDate: LocalDate? = null

    private var deadlineAdapter: DeadlineAdapter? = null

    private val groupViewModel: GroupViewModel by activityViewModels()
    private val taskViewModel: TaskViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        initControl()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initControl() {
        getData()
        binding.btnBackMonth.setOnClickListener { changeMonth(-1) }
        binding.btnNextMonth.setOnClickListener { changeMonth(1) }
        collectState()
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Lấy danh sách task theo ngày khi bấm chọn
                taskViewModel.taskState.collect { state ->
                    when (state) {
                        is TaskUIState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        else -> Unit
                    }
                }
            }
        }

        // Lấy map số lượng task theo ngày cho badge
        viewLifecycleOwner.lifecycleScope.launch {
            taskViewModel.countByDay.collect { map ->
                binding.calendarView.notifyCalendarChanged()
            }
        }

        // Lấy full list task để lọc theo ngày
        viewLifecycleOwner.lifecycleScope.launch {
            taskViewModel.tasks.collect { list ->
                currentFullTaskList = list

                // Nếu đã chọn ngày rồi → cập nhật lại list UI
                currentDate?.let { selectedDate ->
                    listTask(selectedDate)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setUpRecyclerView(deadlineList: List<Task>) {
        if (deadlineAdapter == null) {
            deadlineAdapter = DeadlineAdapter(inGroup, userId, requireContext()) {
                val intent = Intent(requireContext(), TaskDetailActivity::class.java)
                intent.putExtra("taskId", it.id)
                startActivity(intent)
            }
            binding.recyclerViewListTaskWithDay.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            binding.recyclerViewListTaskWithDay.adapter = deadlineAdapter
        }

        deadlineAdapter?.submitList(deadlineList.toList())
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun changeMonth(offset: Int) {
        if (!::currentMonth.isInitialized) return
        currentMonth = currentMonth.plusMonths(offset.toLong())
        binding.calendarView.setup(currentMonth, currentMonth, DayOfWeek.SUNDAY)
        binding.calendarView.scrollToMonth(currentMonth)
        updateMonthTitle(binding.txtMonth, currentMonth)
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateMonthTitle(txtMonth: TextView, currentMonth: YearMonth) {
        val monthName = currentMonth.month.name.substring(0, 1)
            .uppercase(Locale.getDefault()) + currentMonth.month.name.substring(1)
            .lowercase(Locale.getDefault())
        txtMonth.text = "$monthName ${currentMonth.year}"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getData() {
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "").orEmpty()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupViewModel.group.collect { state ->
                    state?.let {
                        group = it
                        inGroup = true
                    }

                    taskViewModel.loadAllTasks(userId, state?.groupId)
                }
            }
        }

        val today: LocalDate = LocalDate.now()
        currentMonth = YearMonth.from(today)
        setupCalendar(today)
        updateMonthTitle(binding.txtMonth, currentMonth)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCalendar(today: LocalDate) {
        val calendarHelper = CalendarHelper(
            calendarView = binding.calendarView,
            context = requireContext(),
            taskViewModel = taskViewModel,
            getSelectedDate = { selectedDate },
            setSelectedDate = { selectedDate = it },
            getCurrentMonth = { currentMonth },
            onDateSelected = { date -> listTask(date) },
            getFormattedDate = { date -> getFormattedDate(date) }
        )
        calendarHelper.setupCalendar(today)
    }

    private fun listTask(date: String) {
        currentDate = date

        if (currentFullTaskList.isEmpty()) return

        val filtered = currentFullTaskList.filter { task ->
            task.dateOnly[userId] == date
        }

        setUpRecyclerView(filtered)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getFormattedDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return date.format(formatter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
