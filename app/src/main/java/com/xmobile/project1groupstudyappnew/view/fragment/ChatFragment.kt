package com.xmobile.project1groupstudyappnew.view.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.FragmentChatBinding
import com.xmobile.project1groupstudyappnew.helper.UploadFileHandler
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.Message
import com.xmobile.project1groupstudyappnew.model.state.ChatUIState
import com.xmobile.project1groupstudyappnew.model.state.FileUIState
import com.xmobile.project1groupstudyappnew.model.state.UserUIState
import com.xmobile.project1groupstudyappnew.utils.ui.dialog.ConfirmDialog
import com.xmobile.project1groupstudyappnew.view.activity.GroupActivity
import com.xmobile.project1groupstudyappnew.view.activity.activity_preview_file.PreviewFileActivity
import com.xmobile.project1groupstudyappnew.view.adapter.ChatAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.ChatViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.FileViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : BaseFragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private var group: Group? = null
    private lateinit var userId: String
    private var file: File? = null
    private var avatar: String = ""
    private var memberName: String = ""

    private val viewModel: ChatViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val fileViewModel: FileViewModel by viewModels()
    private val groupViewModel: GroupViewModel by activityViewModels()

    private var chatAdapter: ChatAdapter? = null

    private var btnSend: ImageView? = null
    private var edtMessage: EditText? = null
    private var btnAttach: ImageView? = null

    private lateinit var uploadFileHandler: UploadFileHandler

    private var alertDelete: AlertDialog? = null
    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uploadFileHandler = UploadFileHandler(this, requireContext(), fileViewModel)
        uploadFileHandler.initLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        (activity as? GroupActivity)?.let { activity ->
            btnSend = activity.binding.btnSend
            edtMessage = activity.binding.edtMessage
            btnAttach = activity.binding.btnAttach
        }
        initControl()
        return binding.root
    }

    private fun initControl() {
        getData()

        btnSend?.setOnClickListener {
            val message = edtMessage?.text.toString()
            if (message.isNotEmpty() && group != null) {
                viewModel.sendMessage(group!!.groupId, userId, memberName, avatar, message)
                edtMessage?.text?.clear()
            }
        }

        btnAttach?.setOnClickListener { v ->
            if (isAdded) uploadFileHandler.uploadFile(binding.progressBar, v)
        }

        collectStates()
    }

    private fun collectStates() {
        // Chat state
        collectFlow(viewModel.chatState) { state ->
            when (state) {
                ChatUIState.Loading -> showLoading(true)
                is ChatUIState.Error -> showError(state.message)
                ChatUIState.SuccessSendMessage -> showLoading(false)
                is ChatUIState.SuccessListMessage -> {
                    showLoading(false)
                    setUpChatRecyclerView(state.messages)
                }
                else -> Unit
            }
        }

        // Chat messages list
        collectFlow(viewModel.messages) { list ->
            setUpChatRecyclerView(list)
            binding.recyclerViewChat.scrollToPosition(list.lastIndex)
        }

        // File state
        collectFlow(fileViewModel.fileState) { state ->
            when (state) {
                FileUIState.Loading -> showLoading(true)
                is FileUIState.Error -> showError(state.message)
                is FileUIState.SetUriUpload -> {
                    uri = state.uri
                    fileViewModel.checkCapacity(requireContext(), state.uri)
                }
                FileUIState.SuccessUploadToFirebase -> {
                    showLoading(false)
                    group?.let { viewModel.uploadFile(it.groupId, userId, memberName, avatar, file) }
                    Toast.makeText(context, getString(R.string.upload_success), Toast.LENGTH_SHORT).show()
                }
                is FileUIState.SuccessGetFile -> {
                    file = state.file
                    fileViewModel.uploadToFirebase(requireContext(), state.file)
                }
                is FileUIState.SuccessCheckCapacity -> {
                    uri = state.uri
                    group?.let { userViewModel.getMemberInfo(it.groupId, userId) }
                }
                is FileUIState.SuccessDelete -> showLoading(false)
                else -> Unit
            }
        }

        // User state
        collectFlow(userViewModel.userState) { state ->
            when (state) {
                is UserUIState.Error -> Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                is UserUIState.SuccessGetMemberInfo -> {
                    avatar = state.member.avatar
                    memberName = state.member.memberName
                    uri?.let { safeUri ->
                        group?.let { grp ->
                            fileViewModel.getFileAndUploadToCloudinary(
                                requireContext(), safeUri, grp.groupId, userId,
                                state.member.memberName, false
                            )
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) uploadFileHandler.disableUserInteraction()
        else uploadFileHandler.enableUserInteraction()
    }

    private fun showError(message: String) {
        showLoading(false)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun <T> collectFlow(flow: kotlinx.coroutines.flow.Flow<T>, collect: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                flow.collect { collect(it) }
            }
        }
    }

    private fun getData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupViewModel.group.collect { state ->
                    group = state
                    group?.let {
                        viewModel.listMessage(it.groupId)
                        viewModel.listenMessages(it.groupId)
                    }
                }
            }
        }

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "").orEmpty()
    }

    private fun setUpChatRecyclerView(messages: List<Message>) {
        if (!isAdded) return

        if (chatAdapter == null) {
            chatAdapter = ChatAdapter(
                requireContext(),
                userId,
                onItemClick = { it.file?.let { f -> previewNavigation(f) } },
                onLongClick = { if (it.file != null && userId == it.file.uploadedBy) deleteFile(it.file) }
            )
            binding.recyclerViewChat.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewChat.adapter = chatAdapter
        }
        chatAdapter?.submitList(messages.toList())
    }


    private fun deleteFile(file: File) {
        if (!isAdded) return
        ConfirmDialog.showCustomDialog(
            context = requireContext(),
            title = requireContext().getString(R.string.str_delete),
            onConfirm = {
                fileViewModel.deleteFile(file)
            },
            onCancel = null
        )
    }

    private fun previewNavigation(file: File) {
        if (!isAdded) return
        val intent = Intent(requireContext(), PreviewFileActivity::class.java)
        intent.putExtra("file", file)
        intent.putExtra("groupId", file.groupId)
        intent.putExtra("userId", userId)
        startActivity(intent)    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        alertDelete = null
    }
}
