package com.xmobile.project1groupstudyappnew.model.obj.file

import java.io.Serializable

data class File(
    val id: String = "",
    val groupId: String = "",
    val type: Int = 3, // 0: pdf, 1: image, 2: video, 3: other
    val name: String = "",
    val filePath: String = "",
    val url: String = "",
    val previewUrl: String = "",
    val capacity: Long = 0L,
    val inTask: Boolean = false,
    val uploadedBy: String = "", // uid
    val uploadedByName: String = "",
    val uploadedAt: Long = 0L
): Serializable