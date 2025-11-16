package com.xmobile.project1groupstudyappnew.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.database.FirebaseDatabase
import com.xmobile.project1groupstudyappnew.api_service.CloudinaryApi
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.file.FileInfo
import com.xmobile.project1groupstudyappnew.model.obj.Message
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.room.dao.FileDao
import com.xmobile.project1groupstudyappnew.room.entity.FileRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import retrofit2.HttpException
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FileRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val cloudinaryApi: CloudinaryApi,
    private val fileDao: FileDao
) : FileRepository {

    override suspend fun listFile(groupId: String): Result<List<File>> = try {
        val dbRef = firebaseDatabase.getReference("files")
        val snapshot = dbRef.orderByChild("groupId").equalTo(groupId).get().await()
        val files = snapshot.children.mapNotNull { it.getValue(File::class.java)}
        Result.success(files)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkCapacity(context: Context, uri: Uri): Result<Boolean> = try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        val maxSize = when {
            mimeType.startsWith("image") -> 25 * 1024 * 1024L
            mimeType.startsWith("video") -> 300 * 1024 * 1024L
            mimeType == "application/pdf" -> 50 * 1024 * 1024L
            else -> 20 * 1024 * 1024L
        }

        val fileSize = querySize(context, uri)
        if (fileSize <= maxSize) Result.success(true)
        else Result.failure(Exception("Dung lượng file vượt quá giới hạn cho phép"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFileAndUploadToCloudinary(
        context: Context,
        uri: Uri,
        fileId: String,
        groupId: String,
        userId: String,
        userName: String,
        inTask: Boolean
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 1) Lấy thông tin file
            val fileInfo = getFileInfo(context, uri)

            // 2) Copy URI -> cached file (nếu cần)
            val cachedFile = copyUriToCacheSafely(context, uri, fileInfo.displayName)

            // 3) Xác định type
            val type = getFileType(fileInfo.mimeType ?: "application/octet-stream")

            // 4) Lưu trạng thái upload vào Room (status = 0)
            fileDao.insertFile(
                FileRoom(
                    fileId = fileId,
                    name = fileInfo.displayName,
                    url = "",
                    previewUrl = null,
                    type = type,
                    size = fileInfo.size,
                    uploadedBy = userId,
                    uploadedByName = userName,
                    uploadedAt = System.currentTimeMillis(),
                    filePath = cachedFile.absolutePath,
                    status = 0
                )
            )

            // 5) Upload (dùng Retrofit CloudinaryApi)
            val originalUrl = uploadToCloudinaryFromFile(cachedFile)

            // 6) Tạo preview nếu image/video
            val previewUrl = if (type == 1 || type == 2) {
                originalUrl.replace("/upload/", "/upload/w_300/")
            } else originalUrl

            // 7) Cập nhật trạng thái Room (status = 1)
            fileDao.updateFilePath(fileId, cachedFile.absolutePath, 1)

            // 8) Trả kết quả
            Result.success(
                File(
                    id = fileId,
                    groupId = groupId,
                    type = type,
                    name = fileInfo.displayName,
                    filePath = cachedFile.absolutePath,
                    url = originalUrl,
                    previewUrl = previewUrl,
                    capacity = fileInfo.size,
                    inTask = inTask,
                    uploadedBy = userId,
                    uploadedByName = userName,
                    uploadedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Upload failed: ${e.message}", e))
        }
    }

    override suspend fun uploadToFirebase(context: Context, file: File): Result<Boolean> = try {
        val dbRef = firebaseDatabase.getReference("files")
        dbRef.child(file.id).setValue(file).await()
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteFile(fileId: String, groupId: String): Result<Boolean> {
        return try {
            val dbRef = firebaseDatabase.getReference("files")
            val taskRef = firebaseDatabase.getReference("tasks")
            val messageRef = firebaseDatabase.getReference("messages")

            // Xóa file trong Firebase
            dbRef.child(fileId).removeValue().await()

            // Cập nhật tasks (nếu có)
            val groupTasksSnapshot = taskRef.orderByChild("groupId").equalTo(groupId).get().await()
            for (taskSnap in groupTasksSnapshot.children) {
                val task = taskSnap.getValue(Task::class.java)
                if (task != null && task.resource?.containsKey(fileId) == true) {
                    val updatedResource = task.resource.toMutableMap()
                    updatedResource[fileId] = false
                    taskRef.child(task.id).child("resource").setValue(updatedResource).await()
                }
            }

            // Cập nhật messages (nếu có)
            val messageSnapshot = messageRef.orderByChild("groupId").equalTo(groupId).get().await()
            for (msgSnap in messageSnapshot.children) {
                val message = msgSnap.getValue(Message::class.java)
                if (message?.file?.id == fileId) messageRef.child(message.id).removeValue().await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(file: File, absolutePath: String): Result<Boolean> {
        return try {
            val srcFile = java.io.File(absolutePath)
            if (!srcFile.exists()) return Result.failure(Exception("File không tồn tại trong cache"))

            val destDir =
                java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GroupStudy")
            if (!destDir.exists()) destDir.mkdirs()

            val destFile = java.io.File(destDir, file.name.ifBlank { srcFile.name })
            srcFile.inputStream().use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }

            val fileRoom = FileRoom(
                fileId = file.id,
                name = file.name,
                url = file.url,
                previewUrl = file.previewUrl,
                type = file.type,
                size = destFile.length(),
                uploadedBy = file.uploadedBy,
                uploadedByName = file.uploadedByName,
                uploadedAt = file.uploadedAt,
                filePath = destFile.absolutePath,
                status = 3
            )
            fileDao.updateFile(fileRoom)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadFileToCache(
        url: String,
        context: Context
    ): Result<java.io.File> = withContext(Dispatchers.IO) {
         try {
            if (url.isBlank()) throw Exception("URL is blank")

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("HTTP error: ${response.code} ${response.message}")
            }

            val body = response.body ?: throw Exception("Response body is null")
            val fileName = url.substringAfterLast("/", "temp_${System.currentTimeMillis()}.pdf")
            val cacheFile = java.io.File(context.cacheDir, fileName)

            Log.d("DownloadFile", "Downloading to: ${cacheFile.absolutePath}")

            body.byteStream().use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("DownloadFile", "Download successful: ${cacheFile.absolutePath}")
            Result.success(cacheFile)

        } catch (e: Exception) {
            Log.e("DownloadFile", "Failed to download file from $url", e)
            Result.failure(e)
        }
    }

    override suspend fun shareFile(file: File, context: Context): Result<Pair<Int, String>> = try {
        val localFile = fileDao.getFileByUrl(file.url)
        val filePath = localFile?.filePath
        Log.d("ShareFile", "filePath: $filePath")
        if (filePath != null && java.io.File(filePath).exists()) Result.success(1 to filePath)
        else {
            val cachedFile = java.io.File(context.cacheDir, file.name)
            Log.d("ShareFile", "cachedFile: ${cachedFile.absolutePath}")
            if (cachedFile.exists()) Result.success(2 to cachedFile.absolutePath)
            else Result.success(3 to file.url)
        }
    } catch (e: Exception) {
        Log.e("ShareFile", "Failed to share file", e)
        Result.failure(e)
    }

    override suspend fun renameFile(fileId: String, newName: String): Result<Boolean> = try {
        val dbRef = firebaseDatabase.getReference("files")
        dbRef.child(fileId).child("name").setValue(newName).await()
        Log.d("RenameFile", "Renamed file with ID: $fileId to new name: $newName")
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun copyUriToCacheSafely(context: Context, uri: Uri, fileName: String): java.io.File {
        val tempFile = java.io.File(context.cacheDir, fileName)

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Không thể mở stream từ URI: $uri. Hãy kiểm tra quyền đọc file.")

        FileOutputStream(tempFile).use { output ->
            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var total = 0L
            while (true) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                output.write(buffer, 0, bytesRead)
                total += bytesRead
            }
            output.flush()
            if (total == 0L) {
                throw Exception("Copy thất bại: file rỗng (URI không đọc được hoặc quyền chưa cấp)")
            }
        }

        return tempFile
    }

    private fun getFileType(mimeType: String): Int = when {
        mimeType.startsWith("image") -> 1
        mimeType.startsWith("video") -> 2
        mimeType == "application/pdf" -> 0
        else -> 0
    }

    private fun querySize(context: Context, uri: Uri): Long {
        val resolver = context.contentResolver
        var size = 0L
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && idx != -1) size = cursor.getLong(idx)
        }
        if (size <= 0) {
            try {
                resolver.openInputStream(uri)?.use { size = it.available().toLong() }
            } catch (_: Exception) { }
        }
        return size
    }

    private fun getFileInfo(context: Context, uri: Uri): FileInfo {
        val contentResolver = context.contentResolver

        var displayName: String? = null
        var size: Long = 0
        var mimeType: String? = contentResolver.getType(uri)

        // try OpenableColumns
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

        // fallback DocumentFile
        if (displayName.isNullOrBlank()) {
            val doc = DocumentFile.fromSingleUri(context, uri)
            displayName = doc?.name
        }

        // still null -> generate name
        if (displayName.isNullOrBlank()) {
            val ext = when {
                mimeType?.startsWith("image") == true -> ".jpg"
                mimeType?.startsWith("video") == true -> ".mp4"
                mimeType == "application/pdf" -> ".pdf"
                else -> ""
            }
            displayName = "file_${System.currentTimeMillis()}$ext"
        }

        if (mimeType == null) {
            // guess from extension
            mimeType = when (displayName.substringAfterLast('.', "").lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "mp4" -> "video/mp4"
                "pdf" -> "application/pdf"
                else -> "application/octet-stream"
            }
        }

        if (size <= 0) {
            try {
                contentResolver.openInputStream(uri)?.use { size = it.available().toLong() }
            } catch (_: Exception) {}
        }

        return FileInfo(displayName, mimeType, size, null)
    }

    private suspend fun uploadToCloudinaryFromFile(file: java.io.File): String =
        withContext(Dispatchers.IO) {
            if (file.length() == 0L) throw Exception("File rỗng, không thể upload: ${file.absolutePath}")

            val requestFile = file.asRequestBody("multipart/form-data".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val preset = "unsigned_preset".toRequestBody("multipart/form-data".toMediaType())

            val response = try {
                cloudinaryApi.uploadFile(filePart, preset)
            } catch (e: HttpException) {
                throw Exception("HTTP error khi upload: ${e.code()} ${e.message()}", e)
            }

            response.secure_url ?: throw Exception("Không lấy được secure_url từ Cloudinary")
        }
}
