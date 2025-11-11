package com.xmobile.project1groupstudyappnew.view.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.FragmentNotiBinding
import com.xmobile.project1groupstudyappnew.helper.MySwipeHelper
import com.xmobile.project1groupstudyappnew.model.obj.Notification
import com.xmobile.project1groupstudyappnew.model.state.NotificationUIState
import com.xmobile.project1groupstudyappnew.view.activity.GroupDetailActivity
import com.xmobile.project1groupstudyappnew.view.activity.MainActivity
import com.xmobile.project1groupstudyappnew.view.activity.TaskDetailActivity
import com.xmobile.project1groupstudyappnew.view.adapter.NotificationAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotiFragment : BaseFragment() {
    private lateinit var binding: FragmentNotiBinding

    private lateinit var userId: String
    private var listNoti: MutableList<Notification> = mutableListOf()
    private var notiAdapter: NotificationAdapter? = null

    private lateinit var mySwipeHelper: MySwipeHelper

    private val viewModel: NotificationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotiBinding.inflate(inflater, container, false)
        initControl()
        return binding.root
    }

    private fun initControl() {
        getData()
        collectState()
    }

    private fun getData() {
        val sharedPreferences = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "").orEmpty()

        viewModel.listNotification(userId)
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notificationState.collect { state ->
                    when (state) {
                        is NotificationUIState.SuccessList -> {
                            listNoti = state.notifications.toMutableList()
                            setUpRecyclerView()
                        }
                        is NotificationUIState.SuccessDelete -> {
                            listNoti.remove(state.notification)
                            notiAdapter?.submitList(listNoti)
                        }
                        is NotificationUIState.SuccessRead -> {
                            intentNavigation(state.notification)
                        }
                        is NotificationUIState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setUpRecyclerView() {
        if (notiAdapter == null) {
            notiAdapter = NotificationAdapter { noti ->
                if (!noti.read) viewModel.readNotification(noti.id)
                else intentNavigation(noti)
            }
            binding.recyclerViewNoti.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            binding.recyclerViewNoti.adapter = notiAdapter
            attachSwipe()
        }

        notiAdapter?.submitList(listNoti)
    }

    private fun intentNavigation(noti: Notification){
        val intent: Intent? = when (noti.type) {
            "task", "message", "member", "file" -> Intent(requireContext(), GroupDetailActivity::class.java).apply {
                putExtra("groupId", noti.groupId)
            }
            "invite" -> Intent(requireContext(), MainActivity::class.java).apply {
                putExtra("inviteCode", noti.inviteCode)
            }
            "deadline" -> Intent(requireContext(), TaskDetailActivity::class.java).apply {
                putExtra("taskId", noti.taskId)
            }
            else -> null
        }
        intent?.let { startActivity(it) }
    }

    private fun attachSwipe() {
        mySwipeHelper =
            object : MySwipeHelper(requireContext(), binding.recyclerViewNoti, 200) {
                override fun instantiateMyButton(
                    viewHolder: RecyclerView.ViewHolder,
                    buffer: MutableList<MyButton>
                ) {
                    buffer.clear()
                    buffer.add(
                        MyButton(
                            requireContext(),
                            "",
                            30,
                            R.drawable.ic_delete,
                            "#007AFF".toColorInt()
                        ) { pos ->
                            deleteFromSwipe(pos)
                        }
                    )
                }
            }
    }

    private fun deleteFromSwipe(pos: Int) {
        if (pos in listNoti.indices) { // fix crash
            val noti = listNoti[pos]
            viewModel.deleteNotification(noti.id)
        }
    }
}
