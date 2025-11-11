package com.xmobile.project1groupstudyappnew.view.activity

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avatarfirst.avatargenlib.AvatarGenerator
import com.bumptech.glide.Glide
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityProfileDetailBinding
import com.xmobile.project1groupstudyappnew.model.obj.group.GroupProgress
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import com.xmobile.project1groupstudyappnew.model.state.GroupUIState
import com.xmobile.project1groupstudyappnew.model.state.TaskUIState
import com.xmobile.project1groupstudyappnew.model.state.UserUIState
import com.xmobile.project1groupstudyappnew.utils.CalculateProgress.calculateProgress
import com.xmobile.project1groupstudyappnew.utils.RandomColor
import com.xmobile.project1groupstudyappnew.utils.ui.chart.Chart
import com.xmobile.project1groupstudyappnew.view.adapter.LegendGroupAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.TaskViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class ProfileDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileDetailBinding
    private lateinit var user: User
    private lateinit var meId: String
    private var groupId: String? = null
    private var newAvatar: String = ""
    private var newUserName: String = ""
    private var newUserDescription: String = ""
    private lateinit var mapTask: Map<Int, List<Task>>

    private var legendStudyAdapter: LegendGroupAdapter? = null
    private var legendProjectAdapter: LegendGroupAdapter? = null

    private val userViewModel: UserViewModel by viewModels()
    private val groupViewModel: GroupViewModel by viewModels()
    private val taskViewModel: TaskViewModel by viewModels()

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivityResult()
        initControl()
    }

    private fun initActivityResult() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { groupViewModel.uploadAvatarToCloudinary(it, meId) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initControl() {
        getData()

        binding.backLayout.setOnClickListener { finish() }

        binding.imgInvite.setOnClickListener {
            groupId?.let { gid ->
                groupViewModel.inviteMember(gid, user.userId, meId)
            }
        }

        binding.imgModify.setOnClickListener { setEditMode(true) }

        binding.txtDone.setOnClickListener {
            newUserName = binding.edtUserName.text.toString()
            newUserDescription = binding.edtUserDescription.text.toString()
            userViewModel.modifyInfo(newUserName, newUserDescription, newAvatar, meId)
        }

        binding.userInviteCode.setOnClickListener { copyInviteCodeToClipboard() }

        binding.imgCamera.setOnClickListener { changeAvatar() }

        binding.chipAll.setOnClickListener { getProgress(0) }
        binding.chipWeek.setOnClickListener { getProgress(1) }
        binding.chipMonth.setOnClickListener { getProgress(2) }

        collectStateGroup()
        collectStateTask()
        collectStateUser()
    }

    private fun setEditMode(isEditMode: Boolean) {
        binding.apply {
            txtDone.visibility = if (isEditMode) View.VISIBLE else View.GONE
            imgModify.visibility = if (isEditMode) View.GONE else View.VISIBLE
            userInviteCode.visibility = if (isEditMode) View.GONE else View.VISIBLE
            imgCamera.visibility = if (isEditMode) View.VISIBLE else View.GONE
            layoutModifyName.visibility = if (isEditMode) View.VISIBLE else View.GONE
            layoutModifyDescription.visibility = if (isEditMode) View.VISIBLE else View.GONE
            userName.visibility = if (isEditMode) View.GONE else View.VISIBLE
            userDescription.visibility = if (isEditMode) View.GONE else View.VISIBLE

            if (isEditMode) {
                edtUserName.setText(newUserName)
                edtUserDescription.setText(newUserDescription)
            }
        }
    }

    private fun copyInviteCodeToClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Invite Code", user.inviteCode)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Đã sao chép mã mời", Toast.LENGTH_SHORT).show()
    }

    private fun changeAvatar() {
        pickImageLauncher.launch("image/*")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getData() {
        user = intent.getSerializableExtra("user", User::class.java)!!
        groupId = intent.getStringExtra("groupId")
        meId = getSharedPreferences("user", MODE_PRIVATE).getString("userId", "")!!
        userViewModel.getUserInfo(meId)

        newAvatar = user.avatar
        newUserName = user.name
        newUserDescription = user.description

        taskViewModel.mapTaskWithUser(user)
    }

    private fun collectStateTask() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.taskState.collect { state ->
                    when (state) {
                        TaskUIState.Loading -> {
                            binding.progressBarStudy.visibility = View.VISIBLE
                            binding.progressBarProject.visibility = View.VISIBLE
                        }

                        is TaskUIState.SuccessMapTask -> {
                            binding.progressBarStudy.visibility = View.GONE
                            binding.progressBarProject.visibility = View.GONE
                            mapTask = state.mapTask
                            displayDataTask()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun collectStateUser() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.userState.collect { state ->
                    when (state) {
                        is UserUIState.Error -> showToast(state.message)
                        is UserUIState.SuccessGetInfo -> {
                            if (meId == user.userId) {
                                user = state.user
                                newAvatar = user.avatar
                                newUserName = user.name
                                newUserDescription = user.description
                                binding.imgInvite.visibility = View.GONE
                            } else binding.imgInvite.visibility = View.VISIBLE
                            displayData()
                        }
                        UserUIState.ConditionDescription -> showToast(getString(R.string.user_description_is_too_long))
                        UserUIState.ConditionName -> showToast(getString(R.string.user_name_is_too_short))
                        UserUIState.EmptyName -> showToast(getString(R.string.enter_user_name))
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun collectStateGroup() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupViewModel.groupState.collect { state ->
                    when (state) {
                        is GroupUIState.SuccessInviteMember -> showToast("Đã gửi lời mời")
                        is GroupUIState.SuccessUpload -> {
                            newAvatar = state.url
                            Glide.with(this@ProfileDetailActivity).load(newAvatar).into(binding.avtarImage)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayData() {
        if (groupId != null) {
            binding.imgModify.visibility = View.GONE
            binding.cardRecentTask.visibility = View.GONE
        } else binding.imgModify.visibility = View.VISIBLE

        setEditMode(false)

        if (newAvatar.isNotEmpty()) {
            Glide.with(this).load(newAvatar).into(binding.avtarImage)
        } else {
            val drawable = AvatarGenerator.AvatarBuilder(this)
                .setLabel(newUserName)
                .setAvatarSize(120)
                .setTextSize(30)
                .toSquare()
                .setBackgroundColor(RandomColor.getRandomColor())
                .build()
            binding.avtarImage.setImageDrawable(drawable)
        }

        binding.userDescription.text = newUserDescription
        binding.userInviteCode.text = "ID: ${user.inviteCode}"
        binding.userName.text = newUserName
        binding.groupAmount.text = user.groups.size.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun displayDataTask() {
        val taskCompleteAmount = (mapTask[1]?.count { it.status[user.userId] == 3 } ?: 0) +
                (mapTask[2]?.count { it.status[user.userId] == 3 } ?: 0)
        binding.taskCompleteAmount.text = taskCompleteAmount.toString()

        val listTaskRecent = getRecentTasks(mapTask, user.userId)
        if (listTaskRecent.size >= 3) {
            binding.txtTitleTask1.text = "- ${listTaskRecent[0].title}"
            binding.txtTitleTask2.text = "- ${listTaskRecent[1].title}"
            binding.txtTitleTask3.text = "- ${listTaskRecent[2].title}"
        }else {
            binding.txtTitleTask1.text = "- ${listTaskRecent[0].title}"
            binding.txtTitleTask2.visibility = View.GONE
            binding.txtTitleTask3.visibility = View.GONE
        }

        getProgress(0)
    }

    private fun getProgress(time: Int) {
        setUpProgressStudyGroup(time)
        setUpProgressProjectGroup(time)
    }

    private fun setUpProgressStudyGroup(time: Int) {
        val tasks = mapTask[1] ?: return
        displayProgress(calculateProgress(tasks, time), 1)
    }

    private fun setUpProgressProjectGroup(time: Int) {
        val tasks = mapTask[2] ?: return
        displayProgress(calculateProgress(tasks, time), 2)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun displayProgress(progress: GroupProgress, type: Int) {
        val pieChart: PieChart
        val recyclerView: RecyclerView
        if (type == 1) {
            pieChart = binding.pieChartStudyGroup
            recyclerView = binding.recyclerViewStudyGroup
        } else {
            pieChart = binding.pieChartProjectGroup
            recyclerView = binding.recyclerViewProjectGroup
        }

        if (progress.total == 0) {
            pieChart.clear()
            return
        }

        val entries = listOf(
            PieEntry(progress.getTodoPercent(), getString(R.string.to_do)),
            PieEntry(progress.getInProgressPercent(), getString(R.string.in_progress)),
            PieEntry(progress.getDonePercent(), getString(R.string.done))
        )

        val colors = listOf(
            R.color.blue, R.color.yellow, R.color.green
        ).map { ContextCompat.getColor(this, it) }

        Chart.setupPieChart(pieChart, entries, colors)

        val progressList = listOf(
            0 to progress.todoCount,
            1 to progress.inProgressCount,
            3 to progress.doneCount
        )

        val adapter = if (type == 1) {
            legendStudyAdapter?.apply { setFilter(progressList); notifyDataSetChanged() }
                ?: LegendGroupAdapter(progressList, this).also { legendStudyAdapter = it }
        } else {
            legendProjectAdapter?.apply { setFilter(progressList); notifyDataSetChanged() }
                ?: LegendGroupAdapter(progressList, this).also { legendProjectAdapter = it }
        }

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
    }

    private fun getRecentTasks(map: Map<Int, List<Task>>, userId: String): List<Task> {
        val allTasks = map.values.flatten()
        val tasksWithDate = allTasks.filter { it.dateOnly.containsKey(userId) }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        return tasksWithDate.sortedByDescending { task ->
            try {
                task.dateOnly[userId]?.let { dateFormat.parse(it)?.time } ?: 0L
            } catch (_: Exception) {
                0L
            }
        }.take(3)
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
