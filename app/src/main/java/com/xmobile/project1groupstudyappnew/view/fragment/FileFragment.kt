package com.xmobile.project1groupstudyappnew.view.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.FragmentFileBinding
import com.xmobile.project1groupstudyappnew.helper.UploadFileHandler
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.state.FileUIState
import com.xmobile.project1groupstudyappnew.model.state.UserUIState
import com.xmobile.project1groupstudyappnew.utils.ui.dialog.ConfirmDialog
import com.xmobile.project1groupstudyappnew.utils.ui.dialog.CustomDialog
import com.xmobile.project1groupstudyappnew.view.activity.activity_preview_file.PreviewFileActivity
import com.xmobile.project1groupstudyappnew.view.adapter.FileAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.FileViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.GroupViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FileFragment : BaseFragment() {

    private var _binding: FragmentFileBinding? = null
    private val binding get() = _binding!!
    private var group: Group? = null
    private lateinit var userId: String
    private var currentFile: File? = null
    private var txtCheckName: TextView? = null

    private var fileAdapter: FileAdapter? = null

    @SuppressLint("RestrictedApi")
    private var menuBuilder3: MenuBuilder? = null

    private var alertRename: AlertDialog? = null

    private val viewModel: FileViewModel by viewModels()
    private val groupViewModel: GroupViewModel by activityViewModels()
    private val userViewModel: UserViewModel by viewModels()

    private var uri: Uri? = null

    private lateinit var uploadFileHandler: UploadFileHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uploadFileHandler = UploadFileHandler(this, requireContext(), viewModel)
        uploadFileHandler.initLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBinding.inflate(inflater, container, false)
        initControl()
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    private fun initControl() {
        getData()

        binding.chipAll.setOnClickListener { fileAdapter?.filterByType(-1) }
        binding.chipPdf.setOnClickListener { fileAdapter?.filterByType(0) }
        binding.chipImage.setOnClickListener { fileAdapter?.filterByType(1) }
        binding.chipVideo.setOnClickListener { fileAdapter?.filterByType(2) }
        binding.chipOther.setOnClickListener { fileAdapter?.filterByType(3) }

        binding.addFileLayout.setOnClickListener { v ->
            if (isAdded) uploadFileHandler.uploadFile(binding.progressBar, v)
        }

        collectState()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fileState.collect { state ->
                    if (!isAdded) return@collect

                    handleFileState(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.userState.collect { state ->
                    if (!isAdded) return@collect

                    handleUserState(state)
                }
            }
        }
    }

    private fun handleFileState(state: FileUIState) {
        when (state) {
            FileUIState.Loading -> showLoading(true)
            is FileUIState.Error -> showToast(state.message)
            is FileUIState.SetUriUpload -> {
                Log.d("FileFlow", "Current state: $state")
                uri = state.uri
                viewModel.checkCapacity(requireContext(), state.uri)
            }
            is FileUIState.SuccessList -> {
                showLoading(false)
                setUpFileRecyclerView(state.files)
            }
            is FileUIState.SuccessRename -> {
                showToast(R.string.rename_file_success)
                group?.let { viewModel.listFile(it.groupId) }
                showLoading(false)
                alertRename?.dismiss()
            }
            is FileUIState.SuccessDelete -> {
                showToast(R.string.delete_file_success)
                group?.let { viewModel.listFile(it.groupId) }
            }
            FileUIState.SuccessUploadToFirebase -> {
                Log.d("FileFlow", "Current state: $state")
                showLoading(false)
                group?.let { viewModel.listFile(it.groupId) }
                showToast(R.string.upload_success)
            }
            is FileUIState.SuccessGetFile -> {
                Log.d("FileFlow", "Current state: $state")
                viewModel.uploadToFirebase(requireContext(), state.file)
            }
            is FileUIState.SuccessCheckCapacity -> {
                Log.d("CheckFlow", "group=$group, userId=$userId, uri=${state.uri}")
                uri = state.uri
                (viewModel.currentGroup ?: group)?.let { userViewModel.getMemberInfo(it.groupId, userId) }
            }
            FileUIState.EmptyFileName -> {
                txtCheckName?.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.enter_file_name)
                }
            }
            is FileUIState.SuccessGetFileRaw -> {
                showLoading(false)
                proceedShare(state.filePath)
            }
            is FileUIState.SuccessGetFileFromCache -> {
                showLoading(false)
                proceedShare(state.absolutePath)
            }
            is FileUIState.SuccessGetFileUrl -> {
                showLoading(false)
                shareText(state.url)
            }
            else -> Unit
        }
    }

    private fun handleUserState(state: UserUIState) {
        when (state) {
            is UserUIState.Error -> showToast(state.message)
            is UserUIState.SuccessGetMemberInfo -> {
                Log.d("FileFlow", "Current state: $state")

                val safeUri = uri
                val safeGroup = viewModel.currentGroup ?: group

                if (safeUri != null && safeGroup != null && isAdded) {
                    try {
                        viewModel.getFileAndUploadToCloudinary(
                            requireContext(),
                            safeUri,
                            safeGroup.groupId,
                            userId,
                            state.member.memberName,
                            false
                        )
                    } catch (e: Exception) {
                        Log.e("FileFlow", "Error uploading file: ${e.message}")
                    }
                } else {
                    Log.e(
                        "FileFlow",
                        "Cannot proceed upload: safeUri=$safeUri, safeGroup=$safeGroup, isAdded=$isAdded"
                    )
                }
            }
            else -> Unit
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) uploadFileHandler.disableUserInteraction() else uploadFileHandler.enableUserInteraction()
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(context, getString(messageResId), Toast.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun shareText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context?.startActivity(Intent.createChooser(shareIntent, "Chia sẻ liên kết"))
    }

    private fun getData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groupViewModel.group.collect { state ->
                    state?.let {
                        Log.d("FileFlow", "Current group: $it")
                        group = it
                        viewModel.currentGroup = it
                        viewModel.listFile(it.groupId)
                    }
                }
            }
        }

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "").orEmpty()
    }

    private fun setUpFileRecyclerView(files: List<File>) {
        if (!isAdded) return
        if (fileAdapter == null) {
            fileAdapter = FileAdapter(
                onItemClick = { previewNavigation(it) },
                onItemLongClick = { file, anchorView -> modifyFile(file, anchorView) }
            )
            binding.recyclerViewFile.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewFile.adapter = fileAdapter
        }
        fileAdapter?.submitAllFiles(files)
    }

    @SuppressLint("RestrictedApi")
    private fun modifyFile(file: File, anchorView: View) {
        if (!isAdded) return
        menuBuilder3 = MenuBuilder(requireContext())
        val inflater = requireActivity().menuInflater
        inflater.inflate(R.menu.menu_modify_file, menuBuilder3)
        val menuOption = MenuPopupHelper(requireContext(), menuBuilder3!!, anchorView)
        menuOption.gravity = Gravity.CENTER
        menuOption.setForceShowIcon(true)

        menuBuilder3?.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.menu_rename_file -> renameFile(file)
                    R.id.menu_share_file -> shareFile(file)
                    R.id.menu_delete_file -> deleteFile(file)
                }
                return true
            }

            override fun onMenuModeChange(menu: MenuBuilder) {}
        })
        menuOption.show()
    }

    private fun previewNavigation(file: File) {
        if (!isAdded) return
        val intent = Intent(requireContext(), PreviewFileActivity::class.java)
        intent.putExtra("file", file)
        intent.putExtra("groupId", file.groupId)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun renameFile(file: File) {
        if (!isAdded) return

        val onlyNameFile = file.name.substringBeforeLast(".")
        val extension = file.name.substring(file.name.lastIndexOf("."))

        CustomDialog.showCustomDialog(
            context = requireContext(),
            layoutResId = R.layout.layout_rename_file,
            bindViews = { view ->
                // Bind views
                val edtFileName = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.edtFileName)
                edtFileName.editText?.setText(onlyNameFile)
                txtCheckName = view.findViewById(R.id.checkFileName)
            },
            onClickActions = { view, dialog ->
                val txtCancel = view.findViewById<TextView>(R.id.txtCancel)
                val txtConfirm = view.findViewById<TextView>(R.id.txtDone)
                val edtFileName = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.edtFileName)

                txtCancel.setOnClickListener { dialog.dismiss() }
                txtConfirm.setOnClickListener {
                    val newName = edtFileName.editText?.text.toString() + extension
                    viewModel.renameFile(file, newName)
                }
            }
        )
    }

    private fun shareFile(file: File) {
        currentFile = file
        if (isAdded) viewModel.shareFile(file, requireContext())
    }

    private fun proceedShare(filePath: String) {
        if (!isAdded) return
        val localFile = java.io.File(filePath)
        if (localFile.exists()) {
            val fileUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", localFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = when (currentFile?.type) {
                    0 -> "application/pdf"
                    1 -> "image/*"
                    2 -> "video/*"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ tệp qua"))
        }
    }

    private fun deleteFile(file: File) {
        if (!isAdded) return
        ConfirmDialog.showCustomDialog(
            context = requireContext(),
            title = requireContext().getString(R.string.str_delete),
            onConfirm = {
                viewModel.deleteFile(file)
            },
            onCancel = null
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        alertRename = null
    }
}
