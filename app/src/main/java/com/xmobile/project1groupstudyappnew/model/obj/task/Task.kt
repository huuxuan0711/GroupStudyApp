package com.xmobile.project1groupstudyappnew.model.obj.task

import java.io.Serializable

data class Task(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val title: String = "",
    val description: String = "",
    val createdBy: String = "", // uid
    val nameCreatedBy: String = "",
    val assignedTo: String = "", // uid
    val nameAssignedTo: String = "",
    val typeDeadline: Int = 0, //0: sau khi bắt đầu, 1: ngày + giờ, 2: ngày
    val deadline: String = "",
    val quantity: Int = 0, //số lượng
    val type: Int = 0, //1: giờ, 2: ngày, 3: tuần, 4: tháng
    val dateOnly: Map<String, String> = emptyMap(), //assignedTo + date
    val hourOnly: String = "", //Cả ngày hoặc giờ cụ thể
    val deadlineNotified: Map<String, Boolean> = emptyMap(),
    val status: Map<String, Int> = emptyMap(), //assignedTo + status 0: to do, 1: in progress, 2: review, 3: done, 4: canceled
    //đối với nhóm học tập: 1 task chứa nhiều member, còn nhóm dự án thì 1 task chỉ có 1 member
    val createdAt: Long = 0L,
    val resource: Map<String, Boolean>? = emptyMap() // Map<resourceId, isAttached>
): Serializable