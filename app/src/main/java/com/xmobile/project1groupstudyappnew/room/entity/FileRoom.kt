package com.xmobile.project1groupstudyappnew.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileRoom (
    @PrimaryKey val fileId: String,
    val name: String,
    val url: String,
    val previewUrl: String?,
    val type: Int,
    val size: Long,
    val uploadedBy: String,
    val uploadedByName: String,
    val uploadedAt: Long,
    val filePath: String?, // null nếu chưa tải hoặc bị xóa
    val status: Int // 0: uploading, 1: uploaded, 2: downloading, 3: downloaded
)