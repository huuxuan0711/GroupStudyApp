package com.xmobile.project1groupstudyappnew.model.obj.member

data class MemberProgress(
    val memberName: String,
    val doneCount: Int,
    val inProgressCount: Int,
    val notStartedCount: Int,
    val totalCount: Int,
    val avatarUrl: String? = null
) {
    val progressPercent: Float
        get() = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount * 100f
}

