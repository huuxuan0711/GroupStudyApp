package com.xmobile.project1groupstudyappnew.view.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.avatarfirst.avatargenlib.AvatarGenerator
import com.bumptech.glide.Glide
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.FragmentHomeBinding
import com.xmobile.project1groupstudyappnew.helper.MySwipeHelper
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.state.HomeUIState
import com.xmobile.project1groupstudyappnew.utils.RandomColor
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader
import com.xmobile.project1groupstudyappnew.utils.ui.dialog.CustomDialog
import com.xmobile.project1groupstudyappnew.view.activity.*
import com.xmobile.project1groupstudyappnew.view.adapter.GroupAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var dialogGroupInfo: AlertDialog? = null
    private var dialogJoinGroup: AlertDialog? = null
    private var dialogAddGroup: AlertDialog? = null

    private val viewModel: HomeViewModel by viewModels()
    private var userId = ""
    private var listGroup: List<Group> = listOf()
    private var groupAdapter: GroupAdapter? = null

    private var txtCheckName: TextView? = null
    private var txtCheckDescription: TextView? = null
    private var txtCheckId: TextView? = null

    private lateinit var mySwipeHelper: MySwipeHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        initControl()
        return binding.root
    }

    private fun initControl() {
        getUserData()
        binding.addGroup.setOnClickListener { showAddGroupDialog() }
        binding.btnJoin.setOnClickListener { showJoinGroupDialog() }
        binding.searchLayout.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        collectHomeState()
        collectInviteState()
        collectGroupUpdates()
    }

    private fun getUserData() {
        val prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE)
        userId = prefs.getString("userId", "").orEmpty()
        viewModel.listGroup(userId)
    }

    private fun collectHomeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.homeState.collect { state ->
                    if (!isAdded) return@collect
                    when (state) {
                        is HomeUIState.SuccessList -> handleSuccessList(state.groups)
                        is HomeUIState.SuccessCreate -> handleNewGroup(state.group, dialogAddGroup)
                        is HomeUIState.SuccessJoin -> handleNewGroup(state.group, dialogGroupInfo)
                        is HomeUIState.SuccessCheckExist -> showGroupInfoDialog(state.group)
                        is HomeUIState.ConditionName -> txtCheckName?.showError(R.string.group_name_is_too_long)
                        is HomeUIState.ConditionDescription -> txtCheckDescription?.showError(R.string.group_description_is_too_long)
                        is HomeUIState.EmptyGroupId -> txtCheckId?.showError(R.string.enter_group_id)
                        is HomeUIState.EmptyName -> txtCheckName?.showError(R.string.enter_group_name)
                        is HomeUIState.Error -> handleError(state.message)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun handleSuccessList(groups: List<Group>) {
        listGroup = groups

        val lastSeenMap = listGroup.associate { it.groupId to (it.lastSeenMessageId ?: "") }
        viewModel.setLastSeenMap(lastSeenMap)

        setupRecyclerView()

        listGroup.forEach { viewModel.listenGroupUpdates(it.groupId) }
    }

    private fun handleNewGroup(group: Group, dialog: AlertDialog?) {
        dialog?.dismiss()
        listGroup = listGroup + group

        val map = viewModel.lastSeenMessageMap.value.toMutableMap()
        map[group.groupId] = group.lastSeenMessageId ?: ""
        viewModel.setLastSeenMap(map)

        groupAdapter?.submitGroups(listGroup, viewModel.latestMessageMap.value, viewModel.lastSeenMessageMap.value)
        viewModel.listenGroupUpdates(group.groupId)
    }

    private fun handleError(message: String) {
        dialogAddGroup?.dismiss()
        dialogJoinGroup?.dismiss()
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun TextView.showError(@StringRes resId: Int) {
        visibility = View.VISIBLE
        text = getString(resId)
    }

    private fun collectInviteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.groupInvite.collect { code ->
                    if (code.isNotEmpty()) viewModel.checkGroupExist(userId, code)
                }
            }
        }
    }

    private fun collectGroupUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.groupUpdate.collect { updateMap ->
                    // Cập nhật latestMessageMap
                    updateMap.forEach { (groupId, message) ->
                        viewModel.updateLatestMessage(groupId, message)
                    }
                    // Cập nhật adapter để badge “Tin mới” hiển thị
                    groupAdapter?.submitGroups(listGroup, viewModel.latestMessageMap.value, viewModel.lastSeenMessageMap.value)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        if (!isAdded) return
        if (groupAdapter == null) {
            groupAdapter = GroupAdapter(requireContext()) { group ->
                viewModel.markGroupAsRead(group.groupId)
                startActivity(Intent(requireContext(), GroupActivity::class.java).apply {
                    putExtra("group", group)
                })
            }
            binding.recyclerViewGroup.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewGroup.adapter = groupAdapter
            attachSwipe()
        }

        groupAdapter?.submitGroups(
            listGroup,
            viewModel.latestMessageMap.value ?: emptyMap(),
            viewModel.lastSeenMessageMap.value ?: emptyMap()
        )

        binding.recyclerViewGroup.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewGroup.adapter = groupAdapter
        attachSwipe()
    }

    private fun attachSwipe() {
        mySwipeHelper = object : MySwipeHelper(requireContext(), binding.recyclerViewGroup, 200) {
            override fun instantiateMyButton(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, buffer: MutableList<MyButton>) {
                buffer.clear()
                buffer.add(MyButton(requireContext(), "", 30, R.drawable.ic_out, "#FF0000".toColorInt()) { outGroup(it) })
                buffer.add(MyButton(requireContext(), "", 30, R.drawable.ic_share, "#252836".toColorInt()) { shareGroup(it) })
                buffer.add(MyButton(requireContext(), "", 30, R.drawable.ic_invite, "#007AFF".toColorInt()) { inviteMember(it) })
            }
        }
    }

    private fun showGroupInfoDialog(group: Group) {
        CustomDialog.showCustomDialog(requireContext(), R.layout.layout_group_info,
            bindViews = { view ->
                val txtName = view.findViewById<TextView>(R.id.group_name)
                val txtCategory = view.findViewById<TextView>(R.id.group_category)
                val txtSize = view.findViewById<TextView>(R.id.group_size)
                val txtSlogan = view.findViewById<TextView>(R.id.group_slogan)
                val avatar = view.findViewById<ImageView>(R.id.group_image)

                txtName.text = group.name
                txtCategory.text = if (group.type == 1) getString(R.string.study) else getString(R.string.project)
                txtSize.text = group.size.toString()
                txtSlogan.text = group.description

                AvatarLoader.load(requireContext(), avatar, group.avatar, group.name)
            },
            onClickActions = { view, dialog ->
                view.findViewById<ImageView>(R.id.imgClose).setOnClickListener { dialog.dismiss() }
                view.findViewById<TextView>(R.id.btnJoin).setOnClickListener {
                    viewModel.joinGroup(userId, group)
                }
            }
        )
    }

    private fun showAddGroupDialog() {
        CustomDialog.showCustomDialog(requireContext(), R.layout.layout_addgroup,
            bindViews = { view ->
                txtCheckName = view.findViewById(R.id.checkName)
                txtCheckDescription = view.findViewById(R.id.checkDescription)
            },
            onClickActions = { view, dialog ->
                val edtName = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtNameGroup)
                val edtDesc = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtDescriptionGroup)

                view.findViewById<TextView>(R.id.txtCancel).setOnClickListener { dialog.dismiss() }
                view.findViewById<TextView>(R.id.txtAddFolder).setOnClickListener {
                    val name = edtName.text.toString()
                    val desc = edtDesc.text.toString()
                    val type = if (view.findViewById<RadioButton>(R.id.radioStudy).isChecked) 1 else 2
                    viewModel.createGroup(name, desc, type, userId)
                }

                dialog.setOnDismissListener {
                    edtName.text?.clear()
                    edtDesc.text?.clear()
                    txtCheckName?.visibility = View.GONE
                    txtCheckDescription?.visibility = View.GONE
                }
            }
        )
    }

    private fun showJoinGroupDialog() {
        CustomDialog.showCustomDialog(requireContext(), R.layout.layout_joingroup,
            bindViews = { view ->
                txtCheckId = view.findViewById(R.id.checkId)
            },
            onClickActions = { view, dialog ->
                val edtId = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtIdGroup)
                view.findViewById<TextView>(R.id.txtCancel).setOnClickListener { dialog.dismiss() }
                view.findViewById<TextView>(R.id.txtAddFolder).setOnClickListener {
                    val inviteCode = edtId.text.toString()
                    viewModel.checkGroupExist(userId, inviteCode)
                }

                dialog.setOnDismissListener {
                    edtId.text?.clear()
                    txtCheckId?.visibility = View.GONE
                }
            }
        )
    }


    private fun inviteMember(pos: Int) {
        if (pos !in listGroup.indices) return
        val group = listGroup[pos]
        startActivity(Intent(requireContext(), SearchActivity::class.java).apply {
            putExtra("isAddMember", true)
            putExtra("groupId", group.groupId)
        })
    }

    private fun shareGroup(pos: Int) {
        if (pos !in listGroup.indices) return
        val group = listGroup[pos]
        startActivity(Intent(requireContext(), ShareGroupActivity::class.java).apply {
            putExtra("group", group)
        })
    }

    private fun outGroup(pos: Int) {
        if (pos !in listGroup.indices) return
        val group = listGroup[pos]
        startActivity(Intent(requireContext(), GroupDetailActivity::class.java).apply {
            putExtra("group", group)
            putExtra("isOutGroupFromSwipe", true)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        dialogGroupInfo?.dismiss(); dialogGroupInfo = null
        dialogJoinGroup?.dismiss(); dialogJoinGroup = null
        dialogAddGroup?.dismiss(); dialogAddGroup = null
    }
}
