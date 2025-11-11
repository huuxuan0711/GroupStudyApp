package com.xmobile.project1groupstudyappnew.model.obj.task

import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.file.File

data class TaskInput(
    val title: String,
    val description: String,
    val typeDeadline: Int, // 0 = amount, 1 = day/hour, 2 = day
    val dateOnly: String,
    val hourOnly: String,
    val state: Int,
    val quantity: Int,
    val type: Int,
    val assignedTo: String,
    val listFile: MutableList<File>?,
    val group: Group,
    val userId: String,
    val deadlineDisplay: String // đã convert unit string ở UI/UseCase
)

