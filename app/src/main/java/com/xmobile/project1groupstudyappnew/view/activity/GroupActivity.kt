package com.xmobile.project1groupstudyappnew.view.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.avatarfirst.avatargenlib.AvatarGenerator
import com.bumptech.glide.Glide
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityGroupBinding
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.state.GroupUIState
import com.xmobile.project1groupstudyappnew.utils.RandomColor
import com.xmobile.project1groupstudyappnew.view.adapter.NavigationGroupAdapter
import com.xmobile.project1groupstudyappnew.view.fragment.*
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader
import com.xmobile.project1groupstudyappnew.viewmodel.HomeViewModel

@AndroidEntryPoint
class GroupActivity : BaseActivity() {

    lateinit var binding: ActivityGroupBinding
    private var group: Group? = null
    private var userId: String = ""
    private lateinit var navAdapter: NavigationGroupAdapter

    private val viewModel: GroupViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        getData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initControl()
    }

    private fun initControl() {
        getData()

        binding.backLayout.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.cardInfo.setOnClickListener {
            group?.let {
                val intent = Intent(this, GroupDetailActivity::class.java)
                intent.putExtra("group", it)
                startActivity(intent)
            }
        }

        collectState()
    }

    private fun getData() {
        val intent = intent
        val sharedPreferences = getSharedPreferences("user", MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "") ?: ""

        // Nhận group từ intent
        group = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("group", Group::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("group") as? Group
        }

        // Nếu có groupId thì lấy từ server
        intent.getStringExtra("groupId")?.let { groupId ->
            viewModel.getGroupFromId(groupId)
            homeViewModel.markGroupAsRead(groupId)
        }

        // Đánh dấu notification nếu có
        intent.getStringExtra("notificationId")?.let {
            notificationViewModel.readNotification(it)
        }

        // Nếu group đã có sẵn thì set vào ViewModel
        group?.let {
            viewModel.setGroup(it)
            displayData(it)
            setUpNavigation(it.type)
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.groupState.collect { state ->
                    when (state) {
                        is GroupUIState.SuccessGetGroup -> {
                            group = state.group
                            viewModel.setGroup(state.group) // đảm bảo ViewModel cập nhật
                            displayData(state.group)
                            setUpNavigation(state.group.type)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun displayData(group: Group) {
        // Avatar
        AvatarLoader.load(this, binding.groupImage, group.avatar, group.name)

        binding.groupName.text = group.name

        // Mô tả
        binding.groupSlogan.text = group.description

        // Loại
        binding.groupCategory.text = if (group.type == 1) getString(R.string.study) else getString(R.string.project)

        // Số lượng thành viên
        binding.groupSize.text = group.size.toString()
    }

    private fun setUpNavigation(type: Int) {
        val navItems = createNavList(type)
        navAdapter = NavigationGroupAdapter(navItems, this) { position ->
            navAdapter.setSelectedItem(position)
            replaceFragment(position)
        }
        binding.recyclerViewNav.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewNav.adapter = navAdapter
        binding.recyclerViewNav.isNestedScrollingEnabled = false
        binding.recyclerViewNav.setHasFixedSize(true)

        // Mặc định mở fragment đầu tiên
        if (navItems.isNotEmpty()) replaceFragment(0)
    }

    private fun createNavList(type: Int): List<String> {
        val list = mutableListOf<String>()
        list.add(if (type == 1) getString(R.string.task_study) else getString(R.string.task_project))
        list.add(getString(R.string.chat))
        list.add(getString(R.string.calendar))
        list.add(getString(R.string.progress))
        list.add(getString(R.string.document))
        return list
    }

    private fun replaceFragment(position: Int) {
        val fragment = when (position) {
            0 -> TaskFragment()
            1 -> ChatFragment()
            2 -> CalendarFragment()
            3 -> ProgressFragment()
            4 -> FileFragment()
            else -> TaskFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit() // Không add to backstack

        if (position == 1){
            binding.layoutChat.visibility = android.view.View.VISIBLE
        } else {
            binding.layoutChat.visibility = android.view.View.GONE
        }
    }
}
