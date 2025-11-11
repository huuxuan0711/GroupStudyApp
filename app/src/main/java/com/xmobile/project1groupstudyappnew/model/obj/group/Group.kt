package com.xmobile.project1groupstudyappnew.model.obj.group

import java.io.Serializable

data class Group(
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val type: Int = 1, // 1 cho học tập, 2 cho dự án
    val avatar: String = "",
    val ownerId: String = "",
    val size: Int = 0,
    val lastSeenMessageId: String = "",
    val createdAt: Long = 0L,
    val inviteCode: String = ""
): Serializable