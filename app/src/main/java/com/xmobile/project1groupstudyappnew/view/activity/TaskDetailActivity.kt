package com.xmobile.project1groupstudyappnew.view.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityTaskDetailBinding
import com.xmobile.project1groupstudyappnew.helper.task_helper.TaskDetailUIHelper
import com.xmobile.project1groupstudyappnew.helper.UploadFileHandler
import com.xmobile.project1groupstudyappnew.helper.task_helper.FileHandler
import com.xmobile.project1groupstudyappnew.helper.task_helper.MemberHandler
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.task.TaskInput
import com.xmobile.project1groupstudyappnew.model.state.GroupUIState
import com.xmobile.project1groupstudyappnew.model.state.TaskUIState
import com.xmobile.project1groupstudyappnew.utils.ui.datetime.DateTimePickerHelper
import com.xmobile.project1groupstudyappnew.utils.ui.dialog.ConfirmDialog
import com.xmobile.project1groupstudyappnew.utils.ui.menu_popup.DeadlineTypePopupHelper
import com.xmobile.project1groupstudyappnew.utils.ui.menu_popup.PopupMenuHelper
import com.xmobile.project1groupstudyappnew.view.activity.activity_preview_file.PreviewFileActivity
import com.xmobile.project1groupstudyappnew.viewmodel.FileViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.HomeViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.NotificationViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.TaskViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.getValue

@AndroidEntryPoint
class TaskDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityTaskDetailBinding
    private var group: Group? = null
    private var task: Task? = null
    private var userId: String? = null
    private var isCreateTask: Boolean = false
    private var isSelectedBtnHour = false
    private var isSelectedBtnDate: Boolean = false
    private var state = 0 //check future date
    private var yearSelected: Int = 0
    private var month: Int = 0
    private var day: Int = 0
    private var typeDeadline: Int = 0
    private var quantity = 0
    private var type = 0
    private var status = -1
    private var assignedTo: String = ""
    private var listFile: MutableList<File> = mutableListOf()
    private var isOwner: Boolean = false
    private var isUpdate: Boolean = false

    private val homeViewModel: HomeViewModel by viewModels()
    private val viewModelUser: UserViewModel by viewModels()
    private val viewModelGroup: GroupViewModel by viewModels()
    private val viewModel: TaskViewModel by viewModels()
    private val fileViewModel: FileViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    private lateinit var uploadFileHandler: UploadFileHandler
    private lateinit var uiHelper: TaskDetailUIHelper
    private lateinit var fileHandler: FileHandler
    private lateinit var memberHandler: MemberHandler

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        uploadFileHandler = UploadFileHandler(this, this, fileViewModel)
        uploadFileHandler.initLaunchers()
        initControl()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        getData()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initControl() {
        getData()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.modifyTask.setOnClickListener { view ->
            modifyTask(view)
        }

        binding.txtDone.setOnClickListener {
            isUpdate = true
            createTask()
        }

        binding.txtCancel.setOnClickListener {
            doneEditTask()
        }

        binding.btnAction.setOnClickListener {
            Log.d("TaskDetailActivity", "initControl: $status")
            if (isCreateTask){
                createTask()
            }else{
                when (status) {
                    0 -> {
                        startTask()
                    }
                    1 -> {
                        doneOrReviewTask()
                    }
                    2 -> {
                        acceptTask()
                    }
                    4 -> {
                        startTask()
                    }
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            cancelTask()
        }

        binding.txtOption.setOnClickListener { view ->
            selectTypeDeadline(view)
        }

        binding.btnHour.setOnClickListener { v ->
            isSelectedBtnHour = !isSelectedBtnHour
            isSelectedBtnDate = false
            updateButtonStyles()
            setupHourPicker()
        }

        binding.btnDate.setOnClickListener { v ->
            isSelectedBtnDate = !isSelectedBtnDate
            isSelectedBtnHour = false
            updateButtonStyles()
            setupDatePicker()
        }

        binding.type.setOnClickListener { v ->
            showOption(v)
        }

        binding.btnAddAssignee.setOnClickListener {
            memberHandler.showBottomSheetMember(binding.root, group!!.groupId, userId!!)
        }

        binding.assigneeName.setOnClickListener {
            binding.assigneeName.visibility = View.GONE
            binding.assigneeName.text = ""
            assignedTo = ""
            binding.btnAddAssignee.visibility = View.VISIBLE
        }

        binding.addFileLayout.setOnClickListener { v ->
            if (!isCreateTask) {
                if (userId == task!!.assignedTo) uploadFileHandler.uploadFile(binding.progressBar, v)
                else fileHandler.showBottomSheetFile(binding.root)
            }else {
                fileHandler.showBottomSheetFile(binding.root)
            }
        }

        fileHandler.initStateListeners()
        memberHandler.initStateListener()
        collectGroupState()
        collectTaskState()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) uploadFileHandler.disableUserInteraction()
        else uploadFileHandler.enableUserInteraction()
    }

    private fun showToast(message: String) {
        Toast.makeText(this@TaskDetailActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun showValidationError(resId: Int) {
        binding.txtError.visibility = View.VISIBLE
        binding.txtError.text = getString(resId)
    }

    private fun resetUI() {
        binding.txtError.visibility = View.GONE
        showLoading(false)
    }

    private fun collectTaskState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.taskState.collect { state ->
                    when (state) {
                        TaskUIState.Loading -> showLoading(true)
                        is TaskUIState.Error -> showToast(state.message).also { showLoading(false) }
                        TaskUIState.ConditionDate -> showValidationError(R.string.please_select_time_again)
                        TaskUIState.EmptyAssignedTo -> showValidationError(R.string.please_select_assignee)
                        TaskUIState.EmptyDescriptionTask -> showValidationError(R.string.enter_task_description)
                        TaskUIState.EmptyNameTask -> showValidationError(R.string.enter_task_name)
                        TaskUIState.EmptyQuantity, TaskUIState.EmptyType -> showValidationError(R.string.please_enter_amount_of_time)
                        is TaskUIState.SuccessCreate -> resetUI().also { finish() }
                        is TaskUIState.SuccessGetListFile -> resetUI().also {
                            listFile.addAll(state.files)
                            fileHandler.setFileInTask(listFile)
                            fileHandler.setUpFileInTaskRecyclerView()
                        }
                        is TaskUIState.SuccessDelete -> resetUI().also { finish() }
                        is TaskUIState.SuccessUpdate -> resetUI().also {
                            doneEditTask()
                            isUpdate = false
                            task = state.task
                            uiHelper.displayData(task, group, isCreateTask, isOwner)
                        }
                        is TaskUIState.SuccessUpdateStatus -> resetUI().also {
                            status = state.status
                            uiHelper.updateWithStatus(task, status)
                        }
                        is TaskUIState.SuccessGetTask -> {
                            task = state.task
                            status = task!!.status[userId!!]!!
                            uiHelper.updateWithStatus(task, status)
                            intent.getStringExtra("notificationId")?.let {
                                homeViewModel.markGroupAsRead(task!!.groupId)
                            }
                            viewModelGroup.getGroupFromId(task!!.groupId)
                            viewModel.listFileWithTask(task!!)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun collectGroupState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelGroup.groupState.collect { state ->
                    when (state) {
                        is GroupUIState.Role -> {
                            isOwner = state.role == 1
                            uiHelper.displayData(task, group, isCreateTask, isOwner)
                            fileHandler.setUploadContext(group!!.groupId, userId!!)
                        }
                        is GroupUIState.SuccessGetGroup -> {
                            group = state.group
                            group?.let { viewModelGroup.checkRole(userId!!, it.groupId) }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun previewNavigation(file: File) {
        val intent = Intent(this, PreviewFileActivity::class.java)
        intent.putExtra("file", file)
        intent.putExtra("groupId", file.groupId)
        intent.putExtra("userId", userId)
        startActivity(intent)

    }

    private fun deleteFile(file: File) {
        ConfirmDialog.showCustomDialog(
            context = this,
            title = getString(R.string.str_delete),
            onConfirm = {
                fileViewModel.deleteFile(file)
            },
            onCancel = null
        )
    }

    private fun startTask() {
        Log.d("TaskDetailActivity", "startTask: $task")
        viewModel.updateStatus(task!!, 1, userId!!)
    }

    private fun doneOrReviewTask() {
        if (group?.type == 1) viewModel.updateStatus(task!!, 3, userId!!)
        else viewModel.updateStatus(task!!, 2, userId!!)
    }

    private fun acceptTask() {
        viewModel.updateStatus(task!!, 3, userId!!)
    }

    private fun cancelTask() {
        ConfirmDialog.showCustomDialog(
            context = this,
            title = getString(R.string.cancel_task),
            onConfirm = {
                viewModel.updateStatus(task!!, 4, userId!!)
            },
            onCancel = null
        )
    }

    @SuppressLint("RestrictedApi")
    private fun modifyTask(anchorView: View) {
        PopupMenuHelper.show(this, menuInflater ,anchorView, R.menu.menu_modify_task, { itemId ->
            when (itemId) {
                R.id.menu_edit_task -> {
                    editTask()
                }
                R.id.menu_delete_task -> {
                    deleteTask()
                }
            }
        })
    }

    private fun editTask() {
        uiHelper.editTask()
    }

    private fun deleteTask() {
        ConfirmDialog.showCustomDialog(
            context = this,
            title = getString(R.string.delete_task),
            onConfirm = {
                viewModel.deleteTask(task!!)
            },
            onCancel = null
        )
    }

    private fun doneEditTask() {
        uiHelper.doneEditTask()
    }

    private fun createTask() {
        val title = binding.edtTaskName.text.toString()
        val description = binding.edtTaskDescription.text.toString()
        var dateOnly = ""
        var hourOnly = ""
        when (typeDeadline) {
            0 -> {
                // amount of time
                quantity = binding.quantity.text.toString().toIntOrNull() ?: 0
            }
            1 -> {
                // day/hour
                dateOnly = "$day/$month/$yearSelected"
                hourOnly = binding.btnHour.text.toString()
                type = 0
                quantity = 0
            }
            2 -> {
                // day
                dateOnly = "$day/$month/$yearSelected"
                hourOnly = ""
                type = 0
                quantity = 0
            }
            else -> Unit
        }

        val unit = when (type) {
            1 -> getString(R.string.hour)
            2 -> getString(R.string.day)
            3 -> getString(R.string.week)
            4 -> getString(R.string.month)
            else -> "unit"
        }

        val deadlineDisplay = when (typeDeadline) {
            0 -> "$quantity $unit ${getString(R.string.after_beginning)}"
            1, 2 -> "$dateOnly $hourOnly"
            else -> ""
        }

        val input = TaskInput(
            title = title,
            description = description,
            typeDeadline = typeDeadline,
            dateOnly = dateOnly,
            hourOnly = hourOnly,
            state = state,
            quantity = quantity,
            type = type,
            assignedTo = assignedTo,
            listFile = listFile,
            group = group!!,
            userId = userId!!,
            deadlineDisplay = deadlineDisplay
        )

        if (isUpdate) viewModel.updateTask(task!!.id, input)
        else viewModel.createTask(input)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun removeFile(file: File) {
        ConfirmDialog.showCustomDialog(
            context = this,
            title = getString(R.string.remove_file),
            onConfirm = {
                listFile.remove(file)
                fileHandler.setFileInTask(listFile)
                fileHandler.setUpFileInTaskRecyclerView()
            },
            onCancel = null
        )
    }

    @SuppressLint("InflateParams")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectTypeDeadline(view: View) {
        DeadlineTypePopupHelper.show(
            context = this,
            anchorView = view,
            currentOption = binding.txtOption.text.toString()
        ) { selectedId ->
            when (selectedId) {
                R.id.txt_amount_of_time -> {
                    binding.txtOption.text = getString(R.string.select_amount_of_time)
                    typeDeadline = 0
                    selectAmountOfTime()
                }
                R.id.txt_choose_day_hour -> {
                    binding.txtOption.text = getString(R.string.select_day_hour)
                    typeDeadline = 1
                    selectDayHour()
                }
                R.id.txt_choose_day -> {
                    binding.txtOption.text = getString(R.string.select_day)
                    typeDeadline = 2
                    selectDay()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectDay() {
        uiHelper.selectDay()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectDayHour() {
        uiHelper.selectDayHour()
    }

    @SuppressLint("DefaultLocale")
    private fun setupHourPicker() {
        DateTimePickerHelper.setupHourPicker(
            binding.wheelHour,
            binding.wheelMinute,
            binding.btnHour
        ) { hour, minute ->
            state = if (isFutureDateTime(yearSelected, month, day, hour, minute)) 1 else 0
        }
    }

    private fun updateButtonStyles() {
        updateStyle(binding.btnHour, isSelectedBtnHour, binding.layoutPickHour)
        updateStyle(binding.btnDate, isSelectedBtnDate, binding.datePicker)
    }

    @SuppressLint("ResourceAsColor")
    private fun updateStyle(button: Button, selected: Boolean, relatedView: View) {
        val currentMode =
            getResources().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        var bgColor = if (selected) R.color.paleBlue else R.color.blue
        var textColor = if (selected) R.color.lightGray else R.color.white

        if (currentMode == Configuration.UI_MODE_NIGHT_YES) {
            bgColor = if (selected) R.color.pale_darkBlue else R.color.darkBlue
            textColor = if (selected) R.color.blue else R.color.white
        }

        button.setBackgroundColor(getColor(bgColor))
        button.setTextColor(getColor(textColor))
        relatedView.visibility = if (selected) View.VISIBLE else View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDatePicker() {
        DateTimePickerHelper.setupDatePicker(
            binding.datePicker,
            binding.btnDate
        ) { year, month, day ->
            yearSelected = year
            this.month = month
            this.day = day
            state = if (isFutureDateTime(year, month - 1, day, binding.wheelHour.getSelectedPosition(), binding.wheelMinute.getSelectedPosition())) 1 else 0
        }
    }

    private fun isFutureDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Boolean {
        val selected = Calendar.getInstance()
        if (typeDeadline == 1) selected.set(year, month, day, hour, minute)
        else if (typeDeadline == 2) selected.set(year, month, day)
        return selected.after(Calendar.getInstance())
    }

    @SuppressLint("RestrictedApi")
    private fun selectAmountOfTime() {
        binding.layoutAmountTime.visibility = View.VISIBLE
        binding.layoutPickDate.visibility = View.GONE
    }

    @SuppressLint("RestrictedApi")
    private fun showOption(anchorView: View) {
        PopupMenuHelper.show(this, menuInflater ,anchorView, R.menu.menu_type_time_deadline, { itemId ->
            when (itemId) {
                R.id.menu_hour -> {
                    binding.type.text = getString(R.string.hour)
                    type = 1
                }
                R.id.menu_day -> {
                    binding.type.text = getString(R.string.day)
                    type = 2
                }
                R.id.menu_week -> {
                    binding.type.text = getString(R.string.week)
                    type = 3
                }
                R.id.menu_month -> {
                    binding.type.text = getString(R.string.month)
                    type = 4
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getData() {
        val sharedPreferences = getSharedPreferences("user", MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "")

        uiHelper = TaskDetailUIHelper(binding, userId)

        fileHandler = FileHandler(
            context = this,
            lifecycleScope = lifecycleScope,
            lifecycle = lifecycle,
            fileViewModel = fileViewModel,
            userViewModel = viewModelUser,
            recyclerViewInTask = binding.recyclerViewFile,
            onPreviewFile = { previewNavigation(it) },
            onDeleteFile = { deleteFile(it) },
            onRemoveFile = { removeFile(it) }
        )

        memberHandler = MemberHandler(
            context = this,
            lifecycleScope = lifecycleScope,
            lifecycle = lifecycle,
            userViewModel = viewModelUser,
            onMemberSelected = { member ->
                binding.assigneeName.visibility = View.VISIBLE
                binding.btnAddAssignee.visibility = View.GONE
                binding.assigneeName.text = member.memberName
                assignedTo = member.userId
            },
            showToast = { showToast(it) }
        )

        val intent = intent
        if (intent.hasExtra("createTask")){
            isCreateTask = intent.getBooleanExtra("createTask", false)
            group = intent.getSerializableExtra("group", Group::class.java)
            task = null
            uiHelper.displayData(null, group, isCreateTask, false)
            fileHandler.setUploadContext(group!!.groupId, userId!!)
        }else {
            if (intent.hasExtra("taskId")){
                isUpdate = false
                isCreateTask = false
                val taskId = intent.getStringExtra("taskId")
                viewModel.getTaskFromId(taskId!!)
                if (intent.hasExtra("notificationId")){
                    val notificationId = intent.getStringExtra("notificationId")
                     notificationViewModel.readNotification(notificationId!!)
                }
            }else {
                group = intent.getSerializableExtra("group", Group::class.java)
                task = intent.getSerializableExtra("task", Task::class.java)
                isOwner = intent.getBooleanExtra("isOwner", false)
                status = task!!.status[userId!!]!!
                viewModel.listFileWithTask(task!!)
                uiHelper.updateWithStatus(task, status)
                uiHelper.displayData(task, group, isCreateTask, isOwner)
                fileHandler.setUploadContext(group!!.groupId, userId!!)
            }
        }
    }
}