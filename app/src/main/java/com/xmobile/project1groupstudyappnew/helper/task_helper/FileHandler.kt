package com.xmobile.project1groupstudyappnew.helper.task_helper

import android.app.Activity
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.state.FileUIState
import com.xmobile.project1groupstudyappnew.model.state.UserUIState
import com.xmobile.project1groupstudyappnew.utils.ui.bottom_sheet.BottomSheetRecyclerView
import com.xmobile.project1groupstudyappnew.view.adapter.FileAdapter
import com.xmobile.project1groupstudyappnew.viewmodel.FileViewModel
import com.xmobile.project1groupstudyappnew.viewmodel.UserViewModel
import kotlinx.coroutines.launch

class FileHandler(
    private val context: Activity,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val lifecycle: Lifecycle,
    private val fileViewModel: FileViewModel,
    private val userViewModel: UserViewModel,
    private val recyclerViewInTask: RecyclerView,
    private val onPreviewFile: (File) -> Unit,
    private val onDeleteFile: (File) -> Unit,
    private val onRemoveFile: (File) -> Unit,
) {
    private var file: File? = null
    private val listFileInTask = mutableListOf<File>()
    private val listFileInGroup = mutableListOf<File>()
    private var fileInTaskAdapter: FileAdapter? = null
    private var fileAdapter: FileAdapter? = null

    private var recyclerViewInGroup: RecyclerView? = null

    private var currentUploadUri: Uri? = null
    private var currentMemberName: String = ""
    private var currentUserId: String = ""
    private var currentGroupId: String = ""
    private var inTask: Boolean = false

    fun setFileInTask(files: List<File>) {
        listFileInTask.clear()
        listFileInTask.addAll(files)
    }

    fun initStateListeners() {
        // file state
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                fileViewModel.fileState.collect { state ->
                    when (state) {
                        is FileUIState.Error -> {
                            showToast(state.message)
                        }
                        is FileUIState.SetUriUpload -> {
                            fileViewModel.checkCapacity(context, state.uri)
                        }
                        is FileUIState.SuccessUploadToFirebase -> {
                            showToast(context.getString(R.string.upload_success))
                            addFileInTask(file!!)
                        }
                        is FileUIState.SuccessGetFile -> {
                            file = state.file
                            fileViewModel.uploadToFirebase(context, state.file)
                        }
                        is FileUIState.SuccessCheckCapacity -> {
                            currentUploadUri = state.uri
                            userViewModel.getMemberInfo(currentGroupId, currentUserId)
                        }
                        is FileUIState.SuccessList -> updateFileInGroup(state.files)
                        is FileUIState.SuccessDelete -> removeFileInTask(state.file)
                        else -> Unit
                    }
                }
            }
        }

        // user state
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.userState.collect { state ->
                    when (state) {
                        is UserUIState.SuccessGetMemberInfo -> {
                            currentMemberName = state.member.memberName
                            inTask = true
                            currentUploadUri?.let { uri ->
                                fileViewModel.getFileAndUploadToCloudinary(
                                    context, uri, currentGroupId,
                                    currentUserId, currentMemberName, inTask
                                )
                            }
                        }
                        is UserUIState.Error -> showToast(state.message)
                        else -> Unit
                    }
                }
            }
        }
    }

    //RecyclerView setup
    fun setUpFileInTaskRecyclerView() {
        if (fileInTaskAdapter == null) {
            fileInTaskAdapter = FileAdapter(
                onItemClick = { file -> onPreviewFile(file) },
                onItemLongClick = { file, _ ->
                    if (file.inTask) onDeleteFile(file) else onRemoveFile(file)
                }
            )
            recyclerViewInTask.layoutManager = LinearLayoutManager(context)
            recyclerViewInTask.adapter = fileInTaskAdapter
        }
        // submit dữ liệu
        fileInTaskAdapter?.submitAllFiles(listFileInTask)
    }

    fun setUpFileInGroupRecyclerView() {
        if (fileAdapter == null) {
            fileAdapter = FileAdapter(
                onItemClick = {},
                onItemLongClick = { _, _ -> }
            )
            recyclerViewInGroup?.layoutManager = LinearLayoutManager(context)
            recyclerViewInGroup?.adapter = fileAdapter
        }
        fileAdapter?.submitAllFiles(listFileInGroup)
    }

    // Thao tác danh sách file
    fun addFileInTask(file: File) {
        listFileInTask.add(file)
        setUpFileInTaskRecyclerView()
    }

    fun removeFileInTask(file: File) {
        listFileInTask.remove(file)
        setUpFileInTaskRecyclerView()
    }

    fun updateFileInGroup(files: List<File>) {
        listFileInGroup.clear()
        listFileInGroup.addAll(files)
        setUpFileInGroupRecyclerView()
    }

    fun setUploadContext(groupId: String, userId: String) {
        currentGroupId = groupId
        currentUserId = userId
    }

    // BottomSheet chọn file từ group
    fun showBottomSheetFile(rootView: View) {
        BottomSheetRecyclerView.showBottomSheet(
            rootView = rootView,
            bottomSheetId = R.id.bottomSheetListFiles,
            recyclerViewId = R.id.recyclerViewFile,
            setRecyclerView = { recyclerView ->
                recyclerViewInGroup = recyclerView
            },
            loadData = { fileViewModel.listFile(currentGroupId) },
            onItemClick = { position ->
                addFileInTask(listFileInGroup[position])
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
