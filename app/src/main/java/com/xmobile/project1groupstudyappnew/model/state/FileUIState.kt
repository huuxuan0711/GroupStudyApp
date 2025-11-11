package com.xmobile.project1groupstudyappnew.model.state

import android.net.Uri
import com.xmobile.project1groupstudyappnew.model.obj.file.File

sealed class FileUIState {
    object Idle : FileUIState()
    object Loading : FileUIState()
    object EmptyFileName : FileUIState()
    data class SetUriUpload(val uri: Uri) : FileUIState()
    data class SuccessCheckCapacity(val uri: Uri): FileUIState()
    data class SuccessGetFile(val file: File) : FileUIState()
    data class SuccessGetFileRaw(val filePath: String) : FileUIState()
    data class SuccessGetFileFromCache(val absolutePath: String) : FileUIState()
    data class SuccessGetFileUrl(val url: String) : FileUIState()
    object SuccessDownloadFile : FileUIState()
    data class SuccessDownloadToCache(val file: java.io.File) : FileUIState()
    data class SuccessList(val files: List<File>) : FileUIState()
    object SuccessUploadToFirebase: FileUIState()
    data class SuccessDelete(val file: File) : FileUIState()
    data class SuccessRename(val file: File) : FileUIState()
    data class Error(val message: String) : FileUIState()
}