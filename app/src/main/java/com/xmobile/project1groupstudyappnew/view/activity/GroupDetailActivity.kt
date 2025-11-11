package com.xmobile.project1groupstudyappnew.view.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityGroupDetailBinding
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.member.Member
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import com.xmobile.project1groupstudyappnew.model.state.GroupUIState
import com.xmobile.project1groupstudyappnew.model.state.UserUIState
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader
import com.xmobile.project1groupstudyappnew.utils.ui.dialog.ConfirmDialog
import com.xmobile.project1groupstudyappnew.utils.ui.menu_popup.PopupMenuHelper
import com.xmobile.project1groupstudyappnew.view.adapter.MemberAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityGroupDetailBinding
    private lateinit var group: Group
    private lateinit var userId: String
    private var isAdmin = false
    private var isOwner = false
    private var isOutGroupFromSwipe = false
    private var hasOpenedProfile = false
    private var newAvatar = ""
    private var newGroupName = ""
    private var newGroupDescription = ""
    private var members: List<Member>? = null
    private var adapter: MemberAdapter? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private val viewModel: GroupViewModel by viewModels()
    private val viewModelUser: UserViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivityResult()
        initControl()
    }

    private fun initActivityResult() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { viewModel.uploadAvatarToCloudinary(it, group.groupId) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initControl() {
        binding.backLayout.setOnClickListener {
            finish()
        }

        binding.cardAddMember.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            intent.putExtra("isAddMember", true)
            intent.putExtra("groupId", group.groupId)
            startActivity(intent)
        }

        binding.cardShare.setOnClickListener {
            val intent = Intent(this, ShareGroupActivity::class.java)
            intent.putExtra("group", group)
            startActivity(intent)
        }

        binding.cardOut.setOnClickListener {
            alertOutGroup()
        }

        getData()
        modifyInfo()
        collectState()
        collectStateMember()
    }

    private fun getData() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("user", MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "") ?: ""

        val intent = intent
        isOutGroupFromSwipe = intent.getBooleanExtra("isOutGroupFromSwipe", false)
        if (isOutGroupFromSwipe) alertOutGroup()

        group = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("group", Group::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("group") as? Group
        } ?: run {
            Toast.makeText(this, "Nhóm không tồn tại", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        newAvatar = group.avatar
        newGroupName = group.name
        newGroupDescription = group.description

        viewModelUser.listMember(group.groupId)
        displayData()
    }

    private fun collectStateMember() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelUser.userState.collect { state ->
                    when (state) {
                        is UserUIState.SuccessListMember -> handleMemberList(state.members)
                        is UserUIState.Error -> showToast(state.message)
                        is UserUIState.SuccessGetInfo -> openUserProfile(state.user)
                        else -> Unit
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.groupState.collect { state ->
                    when (state) {
                        is GroupUIState.Loading -> hideValidation()
                        is GroupUIState.EmptyName -> showNameError(R.string.enter_group_name)
                        is GroupUIState.ConditionName -> showNameError(R.string.group_name_is_too_long)
                        is GroupUIState.ConditionDescription -> showDescriptionError(R.string.group_description_is_too_long)
                        is GroupUIState.Error -> showToast(state.message)
                        is GroupUIState.SuccessUpload -> updateAvatar(state.url)
                        GroupUIState.SuccessLeave -> leaveGroup()
                        is GroupUIState.SuccessModify -> modifySuccess(state.name, state.description)
                        is GroupUIState.Role -> updateRole(state.role)
                        GroupUIState.SuccessAuthorize -> authorizeSuccess()
                        is GroupUIState.SuccessDeleteMember -> deleteMemberSuccess(state.userId)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun handleMemberList(list: List<Member>) {
        members = list
        setUpMemberRecyclerView()
    }

    private fun openUserProfile(user: User) {
        if (!hasOpenedProfile) {
            hasOpenedProfile = true
            val intent = Intent(this, ProfileDetailActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }
    }

    private fun hideValidation() {
        binding.checkName.visibility = View.GONE
        binding.checkDescription.visibility = View.GONE
    }

    private fun showNameError(resId: Int) {
        binding.checkName.visibility = View.VISIBLE
        binding.checkName.text = getString(resId)
    }

    private fun showDescriptionError(resId: Int) {
        binding.checkDescription.visibility = View.VISIBLE
        binding.checkDescription.text = getString(resId)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateAvatar(url: String) {
        newAvatar = url
        Glide.with(this).load(newAvatar).into(binding.groupImage)
    }

    private fun leaveGroup() {
        showToast(getString(R.string.leave_group_success))
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun modifySuccess(name: String, description: String) {
        setEditMode(false, name, description)
    }

    private fun updateRole(role: Int) {
        when (role) {
            1 -> { isOwner = true; binding.imgModify.visibility = View.VISIBLE }
            2 -> { binding.imgModify.visibility = View.GONE }
            3 -> { isAdmin = true; binding.imgModify.visibility = View.VISIBLE }
        }
    }

    private fun authorizeSuccess() {
        showToast(getString(R.string.authorize_success))
        viewModelUser.listMember(group.groupId)
    }

    private fun deleteMemberSuccess(userId: String) {
        showToast(getString(R.string.deleted_member))
        members = members?.filter { it.userId != userId }
        members?.let { adapter?.submitList(it) }
    }

    private fun modifyInfo() {
        viewModel.checkRole(userId, group.groupId)

        binding.imgModify.setOnClickListener {
            setEditMode(true)
        }

        binding.imgChangeAvatar.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.txtDone.setOnClickListener {
            newGroupName = binding.edtNameGroup.editText!!.text.toString()
            newGroupDescription = binding.edtDescriptionGroup.editText!!.text.toString()
            viewModel.modifyInfo(newGroupName, newGroupDescription, newAvatar, group.groupId)
        }
    }

    private fun setEditMode(enabled: Boolean, name: String = newGroupName, description: String = newGroupDescription) {
        binding.txtDone.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.imgChangeAvatar.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.imgModify.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.layoutModifyName.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.layoutModifyDescription.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.groupName.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.groupDescription.visibility = if (enabled) View.GONE else View.VISIBLE

        if (!enabled) {
            newGroupName = name
            newGroupDescription = description
            displayData()
        }
    }

    private fun setUpMemberRecyclerView() {
        if (adapter == null) {
            adapter = MemberAdapter(
                onItemClicked = { member ->
                    if ((isAdmin || isOwner) && member.userId != userId) modifyMember(member, binding.root)
                },
                onImageProfileClicked = { userId ->
                    viewModelUser.getUserInfo(userId)
                }
            )
            binding.recyclerViewMember.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewMember.adapter = adapter
        }

        adapter?.submitList(members)
    }


    @SuppressLint("RestrictedApi")
    private fun modifyMember(member: Member, anchorView: View) {
        PopupMenuHelper.show(this, menuInflater ,anchorView, R.menu.menu_modify_member, { itemId ->
            when (itemId) {
                R.id.menu_authorize -> authorizeMember(member)
                R.id.menu_delelte_member -> deleteMember(member)
            }
        }) { menuBuilder ->
            if (isAdmin) menuBuilder.findItem(R.id.menu_authorize)?.isVisible = false
            if (member.role == 3) menuBuilder.findItem(R.id.menu_authorize)?.title =
                getString(R.string.unauthorize_admin)
        }
    }

    private fun authorizeMember(member: Member) {
        confirmDialog(this, getString(R.string.authorize_admin),
            {
                viewModel.authorizeMember(group.groupId, member.userId, member.role)
            },
            null
        )
    }

    private fun deleteMember(member: Member) {
        confirmDialog(this, getString(R.string.str_delete),
            {
                viewModel.deleteMember(group.groupId, member.userId)
            },
            null
        )
    }

    private fun alertOutGroup() {
        confirmDialog(this, getString(R.string.leave_group),
            {
                viewModel.leaveGroup(userId, group.groupId)
            },
            {
                if (isOutGroupFromSwipe) finish()
            }
        )
    }

    private fun confirmDialog(context: Context, title: String, onConfirm: () -> Unit, onCancel: (() -> Unit)?) {
        ConfirmDialog.showCustomDialog(
            context = context,
            title = title,
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }

    private fun displayData() {
        AvatarLoader.load(this, binding.groupImage, newAvatar, newGroupName)

        binding.groupName.text = newGroupName
        binding.groupDescription.text = newGroupDescription
        binding.edtNameGroup.editText?.setText(newGroupName)
        binding.edtDescriptionGroup.editText?.setText(newGroupDescription)
        binding.memberAmount.text = "(${group.size})"
    }

    override fun onResume() {
        super.onResume()
        hasOpenedProfile = false
    }
}
