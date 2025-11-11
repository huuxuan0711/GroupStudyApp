package com.xmobile.project1groupstudyappnew.model.obj.member

data class Member(
    val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val role: Int = 2, //1 là chủ phòng, 2 là member, 3 là admin
    val joinedAt: Long = 0L,
    val memberName: String = "",
    val avatar: String = ""
)