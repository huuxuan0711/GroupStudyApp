package com.xmobile.project1groupstudyappnew.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivitySearchBinding
import com.xmobile.project1groupstudyappnew.helper.UploadFileHandler
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import com.xmobile.project1groupstudyappnew.model.state.GroupUIState
import com.xmobile.project1groupstudyappnew.model.state.SearchUIState
import com.xmobile.project1groupstudyappnew.view.activity.activity_preview_file.PreviewFileActivity
import com.xmobile.project1groupstudyappnew.view.adapter.FileAdapter
import com.xmobile.project1groupstudyappnew.view.adapter.GroupAdapter
import com.xmobile.project1groupstudyappnew.view.adapter.TaskAdapter
import com.xmobile.project1groupstudyappnew.view.adapter.UserAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.FileViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.HomeViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var groupId: String? = null
    private var userId: String? = null

    private var userAdapter: UserAdapter? = null
    private var groupAdapter: GroupAdapter? = null
    private var taskAdapter: TaskAdapter? = null
    private var fileAdapter: FileAdapter? = null

    private var users: List<User> = emptyList()
    private var groups: List<Group> = emptyList()
    private var tasks: List<Task> = emptyList()
    private var files: List<File> = emptyList()

    private val viewModel: SearchViewModel by viewModels()
    private val groupViewModel: GroupViewModel by viewModels()
    private val fileViewModel: FileViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    private lateinit var uploadFileHandler: UploadFileHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        uploadFileHandler = UploadFileHandler(this, this, fileViewModel)
        uploadFileHandler.initLaunchers()
        initControl()
    }

    @OptIn(FlowPreview::class)
    private fun initControl() {
        getData()

        // Lắng nghe input với debounce
        binding.searchInput.textChanges()
            .debounce(300)
            .onEach { query ->
                val text = query?.toString()?.trim().orEmpty()
                if (text.isNotBlank()) {
                    if (groupId != null) {
                        viewModel.searchUser(text)
                    } else {
                        binding.layoutContent.visibility = View.VISIBLE
                        binding.chipAll.performClick()
                    }
                }
            }.launchIn(lifecycleScope)

        binding.chipAll.setOnClickListener {
            if (binding.searchInput.text.toString().isBlank()) showAllCards()
            else{
                viewModel.searchAll(binding.searchInput.text.toString())
                showAllCards()
            }
        }
        binding.chipPeople.setOnClickListener { binding.viewMorePeople.visibility = View.GONE
            showCard(binding.cardPeople); setUpRecyclerViewPeople(users) }
        binding.chipGroup.setOnClickListener { binding.viewMoreGroups.visibility = View.GONE
            showCard(binding.cardGroup); setUpRecyclerViewGroups(groups) }
        binding.chipTask.setOnClickListener { binding.viewMoreTasks.visibility = View.GONE
            showCard(binding.cardTask); setUpRecyclerViewTasks(tasks) }
        binding.chipFile.setOnClickListener { binding.viewMoreFiles.visibility = View.GONE
            showCard(binding.cardFile); setUpRecyclerViewFiles(files) }

        binding.viewMorePeople.setOnClickListener {
            binding.viewMorePeople.visibility = View.GONE
            binding.chipPeople.performClick()
        }
        binding.viewMoreGroups.setOnClickListener {
            binding.viewMoreGroups.visibility = View.GONE
            binding.chipGroup.performClick()
        }
        binding.viewMoreTasks.setOnClickListener {
            binding.viewMoreTasks.visibility = View.GONE
            binding.chipTask.performClick()
        }
        binding.viewMoreFiles.setOnClickListener {
            binding.viewMoreFiles.visibility = View.GONE
            binding.chipFile.performClick()
        }

        binding.txtCancel.setOnClickListener { finish() }

        collectSearchState()
        collectGroupState()
    }

    // Text changes extension
    fun EditText.textChanges(): Flow<CharSequence?> = callbackFlow {
        val listener = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { trySend(s) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        addTextChangedListener(listener)
        awaitClose { removeTextChangedListener(listener) }
    }

    private fun collectSearchState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchState.collect { state ->
                    when (state) {
                        SearchUIState.Idle -> handleLoading(false)
                        SearchUIState.Loading -> handleLoading(true)
                        is SearchUIState.Error -> {
                            handleLoading(false)
                            showToast(state.message)
                        }
                        is SearchUIState.SuccessSearchUser -> {
                            handleLoading(false)
                            handleUserResult(state.users)
                        }
                        is SearchUIState.SuccessSearchGroup -> {
                            handleLoading(false)
                            handleGroupResult(state.groups)
                        }
                        is SearchUIState.SuccessSearchTask -> {
                            handleLoading(false)
                            handleTaskResult(state.tasks)
                        }
                        is SearchUIState.SuccessSearchFile -> {
                            handleLoading(false)
                            handleFileResult(state.files)
                        }
                    }
                }
            }
        }
    }

    private fun collectGroupState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupViewModel.groupState.collect { state ->
                    when (state) {
                        GroupUIState.Loading -> {
                            handleLoading(true, disableInteraction = true)
                        }
                        is GroupUIState.Error -> {
                            handleLoading(false, disableInteraction = true)
                            showToast(state.message)
                        }
                        is GroupUIState.SuccessInviteMember -> showToast("Đã gửi lời mời")
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun handleLoading(isLoading: Boolean, disableInteraction: Boolean = false) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (disableInteraction) {
            if (isLoading) uploadFileHandler.disableUserInteraction()
            else uploadFileHandler.enableUserInteraction()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@SearchActivity, message, Toast.LENGTH_SHORT).show()
    }


    private fun showCard(selectedCard: ConstraintLayout) {
        val cards = listOf(binding.cardGroup, binding.cardTask, binding.cardFile, binding.cardPeople)
        cards.forEach { it.visibility = if (it == selectedCard) View.VISIBLE else View.GONE }
    }

    private fun showAllCards() {
        val cards = listOf(binding.cardGroup, binding.cardTask, binding.cardFile, binding.cardPeople)
        cards.forEach {
            it.visibility = View.VISIBLE
        }
    }

    private fun handleUserResult(users: List<User>) {
        this.users = users
        if (groupId != null) {
            // Chế độ invite member
            binding.titlePeople.visibility = View.GONE
            binding.viewMorePeople.visibility = View.GONE
            binding.rvPeople.visibility = View.VISIBLE
            setUpRecyclerViewPeople(users)
            return
        }

        if (users.isEmpty()) hideSection(binding.titlePeople, binding.rvPeople, binding.viewMorePeople)
        else {
            binding.titlePeople.visibility = View.VISIBLE
            binding.rvPeople.visibility = View.VISIBLE
            binding.viewMorePeople.visibility = if (users.size > 3) View.VISIBLE else View.GONE
            setUpRecyclerViewPeople(users.take(3))
        }
    }

    private fun handleGroupResult(groups: List<Group>) {
        this.groups = groups
        if (groups.isEmpty()) hideSection(binding.titleGroups, binding.rvGroups, binding.viewMoreGroups)
        else {
            binding.titleGroups.visibility = View.VISIBLE
            binding.rvGroups.visibility = View.VISIBLE
            binding.viewMoreGroups.visibility = if (groups.size > 3) View.VISIBLE else View.GONE
            setUpRecyclerViewGroups(groups.take(3))
        }
    }

    private fun handleTaskResult(tasks: List<Task>) {
        this.tasks = tasks
        if (tasks.isEmpty()) hideSection(binding.titleTasks, binding.rvTasks, binding.viewMoreTasks)
        else {
            binding.titleTasks.visibility = View.VISIBLE
            binding.rvTasks.visibility = View.VISIBLE
            binding.viewMoreTasks.visibility = if (tasks.size > 3) View.VISIBLE else View.GONE
            setUpRecyclerViewTasks(tasks.take(3))
        }
    }

    private fun handleFileResult(files: List<File>) {
        this.files = files
        if (files.isEmpty()) hideSection(binding.titleFiles, binding.rvFiles, binding.viewMoreFiles)
        else {
            binding.titleFiles.visibility = View.VISIBLE
            binding.rvFiles.visibility = View.VISIBLE
            binding.viewMoreFiles.visibility = if (files.size > 3) View.VISIBLE else View.GONE
            setUpRecyclerViewFiles(files.take(3))
        }
    }

    private fun hideSection(vararg views: View) {
        views.forEach { it.visibility = View.GONE }
    }

    private fun setUpRecyclerViewPeople(users: List<User>) {
        if (userAdapter == null) {
            userAdapter = UserAdapter(
                onItemClicked = { user ->
                    if (groupId != null && user.userId != userId)
                        groupViewModel.inviteMember(groupId!!, user.userId, userId)
                    else navigateToProfile(user, groupId)
                },
                onImageProfileClicked = { user -> navigateToProfile(user, groupId) }
            )
            binding.rvPeople.layoutManager = LinearLayoutManager(this)
            binding.rvPeople.adapter = userAdapter
        }

        userAdapter?.submitList(users)
    }

    private fun setUpRecyclerViewGroups(groups: List<Group>) {
        if (groupAdapter == null) {
            groupAdapter = GroupAdapter(this) { group ->
                homeViewModel.markGroupAsRead(group.groupId)
                startActivity(Intent(this, GroupActivity::class.java).apply {
                    putExtra("group", group)
                })
            }
            binding.rvGroups.layoutManager = LinearLayoutManager(this)
            binding.rvGroups.adapter = groupAdapter
        }

        groupAdapter?.submitGroups(
            groups,
            homeViewModel.latestMessageMap.value ?: emptyMap(),
            homeViewModel.lastSeenMessageMap.value ?: emptyMap()
        )
    }

    private fun setUpRecyclerViewTasks(tasks: List<Task>) {
        if (taskAdapter == null) {
            taskAdapter = TaskAdapter(userId!!, 1, this) { task ->
                val intent = Intent(this, TaskDetailActivity::class.java)
                intent.putExtra("taskId", task.id)
                startActivity(intent)
            }
            binding.rvTasks.layoutManager = LinearLayoutManager(this)
            binding.rvTasks.adapter = taskAdapter
        }
        // Chỉ submit danh sách mới
        taskAdapter!!.submitList(tasks.toList())
    }

    private fun setUpRecyclerViewFiles(files: List<File>) {
        if (fileAdapter == null) {
            fileAdapter = FileAdapter(
                onItemClick = { previewNavigation(it) },
                onItemLongClick = { _, _ -> }
            )
            binding.rvFiles.layoutManager = LinearLayoutManager(this)
            binding.rvFiles.adapter = fileAdapter
        }
        fileAdapter?.submitAllFiles(files)
    }

    private fun navigateToProfile(user: User, groupId: String? = null) {
        val intent = Intent(this, ProfileDetailActivity::class.java)
        intent.putExtra("user", user)
        if (groupId != null) intent.putExtra("groupId", groupId)
        startActivity(intent)
    }

    private fun previewNavigation(file: File) {
        val intent = Intent(this, PreviewFileActivity::class.java)
        intent.putExtra("file", file)
        intent.putExtra("groupId", file.groupId)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun getData() {
        if (intent.hasExtra("isAddMember")) groupId = intent.getStringExtra("groupId")
        userId = getSharedPreferences("user", MODE_PRIVATE).getString("userId", null)
        displayData()
    }

    private fun displayData() {
        if (groupId != null) {
            binding.layoutContent.visibility = View.VISIBLE
            binding.chipGroups.visibility = View.GONE
            binding.cardGroup.visibility = View.GONE
            binding.cardTask.visibility = View.GONE
            binding.cardFile.visibility = View.GONE
            binding.cardPeople.visibility = View.VISIBLE
            binding.titlePeople.visibility = View.GONE
            binding.rvPeople.visibility = View.VISIBLE
            binding.searchInput.hint = getString(R.string.enter_user_name_or_id)
        } else {
            binding.searchInput.hint = getString(R.string.search)
        }
    }
}
