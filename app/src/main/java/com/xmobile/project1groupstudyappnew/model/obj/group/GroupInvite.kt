package com.xmobile.project1groupstudyappnew.model.obj.group

data class GroupInvite(
    val id: String = "",
    val inviteCode: String = "",
    val inviterId: String = "",
    val inviteeId: String = "",
    val status: String = "pending", // pending, accepted, declined
    val timestamp: Long = System.currentTimeMillis()
)