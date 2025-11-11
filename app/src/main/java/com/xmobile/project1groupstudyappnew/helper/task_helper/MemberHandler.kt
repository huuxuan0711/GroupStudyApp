package com.xmobile.project1groupstudyappnew.helper.task_helper

import android.app.Activity
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.member.Member
import com.xmobile.project1groupstudyappnew.model.state.UserUIState
import com.xmobile.project1groupstudyappnew.utils.ui.bottom_sheet.BottomSheetRecyclerView
import com.xmobile.project1groupstudyappnew.view.adapter.MemberAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.UserViewModel
import kotlinx.coroutines.launch

class MemberHandler(
    private val context: Activity,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val lifecycle: Lifecycle,
    private val userViewModel: UserViewModel,
    private val onMemberSelected: (Member) -> Unit,
    private val showToast: (String) -> Unit
) {
    private val members = mutableListOf<Member>()
    private var adapterMember: MemberAdapter? = null
    private var recyclerViewMember: RecyclerView? = null

    fun initStateListener() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.userState.collect { state ->
                    when (state) {
                        is UserUIState.SuccessListMember -> {
                            members.clear()
                            members.addAll(state.members)
                            setUpMemberRecyclerView()
                        }
                        is UserUIState.Error -> showToast(state.message)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setUpMemberRecyclerView() {
        if (adapterMember == null) {
            adapterMember = MemberAdapter(
                onItemClicked = { member -> onMemberSelected(member) },
                onImageProfileClicked = { /* xử lý nếu cần */ }
            )
            recyclerViewMember?.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            recyclerViewMember?.adapter = adapterMember
        }

        adapterMember?.submitList(members)
    }

    fun showBottomSheetMember(rootView: View, groupId: String, currentUserId: String) {
        BottomSheetRecyclerView.showBottomSheet(
            rootView = rootView,
            bottomSheetId = R.id.bottomSheetListMembers,
            recyclerViewId = R.id.recyclerViewMember,
            setRecyclerView = { recyclerView ->
                recyclerViewMember = recyclerView
            },
            loadData = { userViewModel.listMember(groupId) },
            onItemClick = { position ->
                val member = members[position]
                if (member.userId != currentUserId) {
                    onMemberSelected(member)
                } else {
                    showToast("Bạn không thể chọn chính mình")
                }
            }
        )
    }
}
