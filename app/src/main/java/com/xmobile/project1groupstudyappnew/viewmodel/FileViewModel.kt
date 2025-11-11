package com.xmobile.project1groupstudyappnew.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.state.FileUIState
import com.xmobile.project1groupstudyappnew.repository.FileRepository
import com.xmobile.project1groupstudyappnew.utils.ValidateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val firebaseDatabase: FirebaseDatabase
): ViewModel() {
    private val _fileState = MutableStateFlow<FileUIState>(FileUIState.Idle)
    val fileState: StateFlow<FileUIState> = _fileState.asStateFlow()
    var currentGroup: Group? = null

    fun setUriUpload(uri: Uri) {
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Loading
                _fileState.value = FileUIState.SetUriUpload(uri)
            } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    fun checkCapacity(context: Context, uri: Uri){
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Loading
                val result = fileRepository.checkCapacity(context, uri)
                result.onSuccess {
                    _fileState.value = FileUIState.SuccessCheckCapacity(uri)
                }
                result.onFailure {
                    _fileState.value = FileUIState.Error(it.message.toString())
                }
            } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    fun listFile(groupId: String) {
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Loading
                val result = fileRepository.listFile(groupId)
                result.onSuccess {
                    _fileState.value = FileUIState.SuccessList(it)
                }
                result.onFailure {
                    _fileState.value = FileUIState.Error(it.message.toString())
                }
            } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    fun getFileAndUploadToCloudinary(context: Context, uri: Uri, groupId: String, userId: String, userName: String, inTask: Boolean) {
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Loading
                val fileId = firebaseDatabase
                    .reference.child("files")
                    .push().key ?: return@launch
                val result = fileRepository.getFileAndUploadToCloudinary(context, uri, fileId, groupId, userId, userName, inTask)
                result.onSuccess {
                    _fileState.value = FileUIState.SuccessGetFile(it)
                }
                result.onFailure {
                    _fileState.value = FileUIState.Error(it.message.toString())
                }
            } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    fun uploadToFirebase(context: Context, file: File) {
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Loading
                val result = fileRepository.uploadToFirebase(context, file)
                result.onSuccess {
                    _fileState.value = FileUIState.SuccessUploadToFirebase
                }
                result.onFailure {
                    _fileState.value = FileUIState.Error(it.message.toString())
                }
            } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Idle
                validateName(newName)
                if (_fileState.value != FileUIState.Idle) {
                    return@launch
                }
                _fileState.value = FileUIState.Loading
                val result = fileRepository.renameFile(file.id, newName)
                result.onSuccess {
                    _fileState.value = FileUIState.SuccessRename(file)
                }
                result.onFailure {
                    _fileState.value = FileUIState.Error(it.message.toString())
                }
            } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    fun shareFile(file: File, context: Context) {
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Loading
                val result = fileRepository.shareFile(file, context)
                result.onSuccess { (check, value) ->
                    when (check) {
                        1 -> {
                            _fileState.value = FileUIState.SuccessGetFileRaw(value)
                        }
                        2 -> {
                            _fileState.value = FileUIState.SuccessGetFileFromCache(value)
                        }
                        else -> {
                            _fileState.value = FileUIState.SuccessGetFileUrl(value)
                        }
                    }
                }
                result.onFailure {
                    _fileState.value = FileUIState.Error(it.message.toString())
                }
            } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Loading
                val result = fileRepository.deleteFile(file.id, groupId = file.groupId)
                result.onSuccess {
                    _fileState.value = FileUIState.SuccessDelete(file)
                }
                result.onFailure {
                    _fileState.value = FileUIState.Error(it.message.toString())
                }
            } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    fun downloadFile(file: File, absolutePath: String) {
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Loading
                val result = fileRepository.downloadFile(file, absolutePath)
                result.onSuccess {
                    _fileState.value = FileUIState.SuccessDownloadFile
                }
                result.onFailure {
                    _fileState.value = FileUIState.Error(it.message.toString())
                }
                } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    fun downloadFileToCache(url: String, context: Context) {
        viewModelScope.launch {
            try {
                _fileState.value = FileUIState.Loading
                val result = fileRepository.downloadFileToCache(url, context)
                result.onSuccess {
                    _fileState.value = FileUIState.SuccessDownloadToCache(it)
                }
                result.onFailure {
                    _fileState.value = FileUIState.Error(it.message.toString())
                }
            } catch (e: Exception) {
                _fileState.value = FileUIState.Error(e.message.toString())
            }
        }
    }

    private fun validateName(name: String){
        if (!ValidateUtil.emptyCheckFileName(name)) {
            _fileState.value = FileUIState.EmptyFileName
        }
    }
}