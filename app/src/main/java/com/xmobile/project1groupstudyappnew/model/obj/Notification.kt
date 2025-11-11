package com.xmobile.project1groupstudyappnew.model.obj

data class Notification(
    val id: String = "",
    val userId: String = "", //người nhận
    val groupId: String = "",
    val taskId: String = "",
    val inviteCode: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", //loại thông báo: task, message, member, file, invite, deadline
    val timestamp: Long = 0L,
    val read: Boolean = false
)