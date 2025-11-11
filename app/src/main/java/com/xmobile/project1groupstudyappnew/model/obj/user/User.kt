package com.xmobile.project1groupstudyappnew.model.obj.user

import java.io.Serializable

data class User(
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val email: String = "",
    val avatar: String = "",
    val groups: Map<String, Boolean> = emptyMap(), // Map<groupId, isJoined>
    val inviteCode: String = ""
): Serializable