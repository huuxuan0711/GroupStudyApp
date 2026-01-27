package com.xmobile.project1groupstudyappnew.view.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityMainBinding
import com.xmobile.project1groupstudyappnew.utils.CacheManager
import com.xmobile.project1groupstudyappnew.view.adapter.Viewpager2Adapter
import com.xmobile.project1groupstudyappnew.viewmodel.HomeViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.NotificationViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val homeViewModel: HomeViewModel by viewModels()
    private val taskViewModel: TaskViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initControl()
    }

    private fun initControl() {
        val userId = getSharedPreferences("user", MODE_PRIVATE).getString("userId", null)
        if (userId == null) {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
            return
        }

        //dọn file trong cache
        CacheManager.clearOldCache(this)

        //kiểm tra task quá hạn
        taskViewModel.updateTaskOverdue(userId ?: "")

        setUpViewPager()
        setUpBottomNavigation()
        handleNavigationFromIntent(intent)
        updateFirebaseToken()
    }

    private fun handleNavigationFromIntent(intent: Intent) {

        // 1️⃣ Mark notification as read (dùng cho mọi type)
        val notificationId = intent.getStringExtra("notificationId")
        if (!notificationId.isNullOrEmpty()) {
            notificationViewModel.readNotification(notificationId)
        }

        // 2️⃣ Lấy type
        val type = intent.getStringExtra("type") ?: return

        when (type) {

            // 💌 Invite
            "invite" -> {
                val inviteCode =
                    intent.getStringExtra("inviteCode")
                        ?: intent.data?.getQueryParameter("inviteCode")

                inviteCode?.let {
                    homeViewModel.setGroupInvite(it)
                }
            }

            // 🕒 Deadline, Task → Task detail
            "deadline", "task" -> {
                val taskId = intent.getStringExtra("taskId") ?: return
                openTaskDetail(taskId)
            }

            // 📌 Message / Member / File → Group
            "message", "member", "file" -> {
                val groupId = intent.getStringExtra("groupId") ?: return
                openGroup(groupId)
            }

            // 🔔 Fallback → Notification tab
            else -> {
                openNotificationTab()
            }
        }

        // ❗ Tránh xử lý lại khi resume
        intent.removeExtra("type")
    }

    private fun openNotificationTab() {
        binding.viewpager2.currentItem = 2 // menu_noti
    }

    private fun openGroup(groupId: String) {
        binding.viewpager2.currentItem = 0 // Home tab
        val intent = Intent(this, GroupActivity::class.java)
        intent.putExtra("groupId", groupId)
        startActivity(intent)
    }

    private fun openTaskDetail(taskId: String) {
        val intent = Intent(this, TaskDetailActivity::class.java)
        intent.putExtra("taskId", taskId)
        startActivity(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationFromIntent(intent)
    }

    private fun updateFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                FirebaseDatabase.getInstance()
                    .getReference("UserTokens")
                    .child(uid)
                    .setValue(token)
            }
        }
    }

    private fun setUpViewPager() {
        val adapter = Viewpager2Adapter(this)
        binding.viewpager2.adapter = adapter
        binding.viewpager2.offscreenPageLimit = 4
        binding.viewpager2.isUserInputEnabled = false

        binding.viewpager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val menuItemId = getMenuIdByPosition(position)
                binding.navigationBar.menu.findItem(menuItemId)?.isChecked = true
            }
        })
    }

    private fun setUpBottomNavigation() {
        binding.navigationBar.setOnItemSelectedListener { item ->
            val position = getViewPagerPositionByMenuId(item.itemId)
            binding.viewpager2.currentItem = position
            true
        }
    }

    private fun getMenuIdByPosition(position: Int): Int = when(position) {
        0 -> R.id.menu_home
        1 -> R.id.menu_calendar
        2 -> R.id.menu_noti
        3 -> R.id.menu_profile
        else -> R.id.menu_home
    }

    private fun getViewPagerPositionByMenuId(menuId: Int): Int = when(menuId) {
        R.id.menu_home -> 0
        R.id.menu_calendar -> 1
        R.id.menu_noti -> 2
        R.id.menu_profile -> 3
        else -> 0
    }
}
