package com.xmobile.project1groupstudyappnew.view.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xmobile.project1groupstudyappnew.databinding.FragmentTaskBinding
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.state.GroupUIState
import com.xmobile.project1groupstudyappnew.model.state.TaskUIState
import com.xmobile.project1groupstudyappnew.view.activity.TaskDetailActivity
import com.xmobile.project1groupstudyappnew.view.adapter.TaskAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TaskFragment : BaseFragment() {

    private lateinit var binding: FragmentTaskBinding
    private var group: Group? = null
    private lateinit var userId: String
    private var listTask: List<Task> = emptyList()
    private var isAdmin: Boolean = false
    private var isOwner: Boolean = false
    private var taskAdapter: TaskAdapter? = null

    private val taskViewModel: TaskViewModel by viewModels()
    private val groupViewModel: GroupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskBinding.inflate(inflater, container, false)
        initControl()
        return binding.root
    }

    private fun initControl() {
        getData()

        binding.addTaskLayout.setOnClickListener { addTask() }

        // Click chip filter status
        binding.chipAll.setOnClickListener { taskAdapter?.listWithStatus(-1) }
        binding.chipTodo.setOnClickListener { taskAdapter?.listWithStatus(0) }
        binding.chipInProgress.setOnClickListener { taskAdapter?.listWithStatus(1) }
        binding.chipReview.setOnClickListener { taskAdapter?.listWithStatus(2) }
        binding.chipDone.setOnClickListener { taskAdapter?.listWithStatus(3) }

        collectState()
        collectStateGroup()
    }

    private fun collectStateGroup() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupViewModel.groupState.collect { state ->
                    when (state) {
                        is GroupUIState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        is GroupUIState.Role -> {
                            isOwner = state.role == 1
                            isAdmin = state.role == 3
                            displayData()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.taskState.collect { state ->
                    when (state) {
                        is TaskUIState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        is TaskUIState.SuccessList -> {
                            listTask = state.tasks.sortedByDescending { it.createdAt }
                            setUpTaskRecyclerView()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun getData() {
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "").orEmpty()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupViewModel.group.collect { state ->
                    state?.let {
                        group = it
                        group?.groupId?.let { it ->
                            groupViewModel.checkRole(userId, it)
                            taskViewModel.listTask(it)
                        }
                    }
                }
            }
        }
    }

    private fun addTask() {
        group?.let { grp ->
            if (grp.type == 2 && !(isAdmin || isOwner)) return
            val intent = Intent(requireContext(), TaskDetailActivity::class.java)
            intent.putExtra("group", grp)
            intent.putExtra("createTask", true)
            startActivity(intent)
        }
    }

    private fun setUpTaskRecyclerView() {
        if (taskAdapter == null) {
            group?.let { grp ->
                taskAdapter = TaskAdapter(userId, grp.type, requireContext()) { task ->
                    handleTaskClick(task)
                }
                binding.recyclerViewTask.layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerViewTask.adapter = taskAdapter
            }
        }
        taskAdapter?.submitList(listTask.toList())
    }

    private fun handleTaskClick(task: Task) {
        group?.let { grp ->
            val canAccess = when (grp.type) {
                1 -> true // group học tập: tất cả truy cập
                2 -> {
                    task.assignedTo == userId || task.createdBy == userId || isOwner
                }
                else -> false
            }

            if (!canAccess) {
                Toast.makeText(requireContext(), "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(requireContext(), TaskDetailActivity::class.java)
            intent.putExtra("task", task)
            intent.putExtra("group", grp)
            intent.putExtra("isOwner", isOwner)
            startActivity(intent)
        }
    }

    private fun displayData() {
        group?.let { grp ->
            binding.chipReview.visibility = if (grp.type == 2) View.VISIBLE else View.GONE
            binding.addTaskLayout.visibility = if (grp.type == 2 && !(isAdmin || isOwner)) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        group?.groupId?.let { taskViewModel.listTask(it) }
    }
}
