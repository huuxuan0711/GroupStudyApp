package com.xmobile.project1groupstudyappnew.repository

import android.content.Context
import android.net.Uri
import com.xmobile.project1groupstudyappnew.model.obj.file.File

interface FileRepository {
    suspend fun listFile(groupId: String): Result<List<File>>
    suspend fun checkCapacity(context: Context ,uri: Uri): Result<Boolean>
    suspend fun getFileAndUploadToCloudinary(context: Context, uri: Uri, fileId: String, groupId: String, userId: String, userName: String, inTask: Boolean): Result<File>
    suspend fun uploadToFirebase(context: Context, file: File): Result<Boolean>
    suspend fun deleteFile(fileId: String, groupId: String): Result<Boolean>
    suspend fun downloadFile(file: File, absolutePath: String): Result<Boolean>
    suspend fun downloadFileToCache(url: String, context: Context): Result<java.io.File>
    suspend fun shareFile(file: File, context: Context): Result<Pair<Int, String>>
    suspend fun renameFile(fileId: String, newName: String): Result<Boolean>
}