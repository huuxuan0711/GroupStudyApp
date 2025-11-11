package com.xmobile.project1groupstudyappnew.view.fragment

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.FragmentProgressBinding
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.group.GroupProgress
import com.xmobile.project1groupstudyappnew.model.obj.member.MemberProgress
import com.xmobile.project1groupstudyappnew.model.state.TaskUIState
import com.xmobile.project1groupstudyappnew.utils.ui.chart.Chart
import com.xmobile.project1groupstudyappnew.view.adapter.LegendGroupAdapter
import com.xmobile.project1groupstudyappnew.view.adapter.LegendMemberAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty0

@AndroidEntryPoint
class ProgressFragment : BaseFragment() {
    private lateinit var binding: FragmentProgressBinding
    private var group: Group? = null
    private lateinit var userId: String

    private var legendGroupAdapter: LegendGroupAdapter? = null
    private var legendMemberAdapter: LegendMemberAdapter? = null

    private val groupViewModel: GroupViewModel by activityViewModels()
    private val taskViewModel: TaskViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgressBinding.inflate(inflater, container, false)
        initControl()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initControl() {
        getData()

        // Chip click: 0 = all, 1 = week, 2 = month
        binding.chipAll.setOnClickListener { getProgress(0) }
        binding.chipWeek.setOnClickListener { getProgress(1) }
        binding.chipMonth.setOnClickListener { getProgress(2) }

        collectState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.taskState.collect { state ->
                    when (state) {
                        is TaskUIState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        TaskUIState.Loading -> {
                            binding.progressBarGroup.visibility = View.VISIBLE
                            binding.progressBarMember.visibility = View.VISIBLE
                        }
                        is TaskUIState.SuccessGetProgressGroup -> {
                            Log.d("ProgressFragment", "SuccessGetProgressGroup: $state")
                            setUpProgressGroup(state.groupProgress)
                            binding.progressBarGroup.visibility = View.GONE
                        }
                        is TaskUIState.SuccessGetProgressMember -> {
                            Log.d("ProgressFragment", "SuccessGetProgressMember: $state")
                            setUpProgressMember(state.memberProgress)
                            binding.progressBarMember.visibility = View.GONE
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun getData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupViewModel.group.collect { state ->
                    state?.let {
                        group = it
                        getProgress(0)
                    }
                }
            }
        }

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "").orEmpty()
    }

    private fun getProgress(time: Int) {
        group?.let {
            taskViewModel.getProgressGroup(time, it.groupId)
            taskViewModel.getProgressMember(time, it.groupId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    private fun setUpProgressGroup(groupProgress: GroupProgress) {
        if (groupProgress.total == 0) {
            binding.pieChartGroup.clear()
            return
        }

        val entries = listOf(
            PieEntry(groupProgress.getTodoPercent(), getString(R.string.to_do)),
            PieEntry(groupProgress.getInProgressPercent(), getString(R.string.in_progress)),
            PieEntry(groupProgress.getDonePercent(), getString(R.string.done))
        )

        val colors = listOf(
            R.color.blue, R.color.yellow, R.color.green
        ).map { ContextCompat.getColor(requireContext(), it) }

        Chart.setupPieChart(binding.pieChartGroup, entries, colors)

        val progressList = listOf(
            0 to groupProgress.todoCount,
            1 to groupProgress.inProgressCount,
            3 to groupProgress.doneCount
        )
        updateLegendAdapter(progressList, ::legendGroupAdapter, binding.recyclerViewProgressGroup) { progressList -> LegendGroupAdapter(progressList, requireContext()) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    private fun setUpProgressMember(memberProgress: List<MemberProgress>) {
        if (memberProgress.isEmpty()) {
            binding.horizontalChartMember.clear()
            return
        }

        val entries = memberProgress.mapIndexed { index, member -> BarEntry(index.toFloat(), member.progressPercent) }
        Chart.setupBarChart(requireContext(), binding.horizontalChartMember, entries, memberProgress.map { it.memberName })

        updateLegendAdapter(memberProgress, ::legendMemberAdapter, binding.recyclerViewProgressMember) { it -> LegendMemberAdapter(it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    private fun <T, A> updateLegendAdapter(
        data: List<T>,
        adapterRef: KMutableProperty0<A?>,
        recyclerView: RecyclerView,
        adapterCreator: (List<T>) -> A
    ) where A : RecyclerView.Adapter<*> {
        adapterRef.get()?.let { adapter ->
            when (adapter) {
                is LegendGroupAdapter -> adapter.setFilter(data as List<Pair<Int, Int>>)
                is LegendMemberAdapter -> adapter.setFilter(data as List<MemberProgress>)
            }
            adapter.notifyDataSetChanged()
        } ?: run {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapterCreator(data).also { adapterRef.set(it) }
        }
    }
}
