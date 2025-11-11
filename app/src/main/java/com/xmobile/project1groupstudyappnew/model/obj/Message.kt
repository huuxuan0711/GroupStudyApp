package com.xmobile.project1groupstudyappnew.model.obj

import com.xmobile.project1groupstudyappnew.model.obj.file.File

data class Message(
    val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val memberName: String = "",
    val avatar: String = "",
    val text: String = "",
    val file: File? = null,
    val timestamp: Long = 0L
)