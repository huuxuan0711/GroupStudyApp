package com.xmobile.project1groupstudyappnew.model.obj.group

data class GroupProgress(
    val groupId: Int,
    val groupName: String,
    val doneCount: Int,
    val inProgressCount: Int,
    val todoCount: Int
) {
    val total: Int get() = doneCount + inProgressCount + todoCount

    fun getDonePercent() = doneCount.toFloat() / total * 100
    fun getInProgressPercent() = inProgressCount.toFloat() / total * 100
    fun getTodoPercent() = todoCount.toFloat() / total * 100
}

