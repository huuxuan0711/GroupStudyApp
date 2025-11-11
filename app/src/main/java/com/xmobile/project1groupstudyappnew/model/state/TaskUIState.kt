package com.xmobile.project1groupstudyappnew.model.state

import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.GroupProgress
import com.xmobile.project1groupstudyappnew.model.obj.member.MemberProgress
import com.xmobile.project1groupstudyappnew.model.obj.task.Task

sealed class TaskUIState {
    object Idle : TaskUIState()
    object Loading : TaskUIState()
    data class SuccessList(val tasks: List<Task>) : TaskUIState()
    data class SuccessMapTask(val mapTask: Map<Int, List<Task>>) : TaskUIState()
    data class SuccessGetListFile(val files: List<File>) : TaskUIState()
    data class SuccessGetTask(val task: Task) : TaskUIState()
    data class SuccessCreate(val task: Task) : TaskUIState()
    data class SuccessDelete(val task: Task) : TaskUIState()
    data class SuccessUpdate(val task: Task) : TaskUIState()
    data class SuccessUpdateStatus(val status: Int) : TaskUIState()
    data class Error(val message: String) : TaskUIState()
    data class SuccessGetProgressGroup(val groupProgress: GroupProgress) : TaskUIState()
    data class SuccessGetProgressMember(val memberProgress: List<MemberProgress>) : TaskUIState()
    object EmptyNameTask: TaskUIState()
    object EmptyDescriptionTask: TaskUIState()
    object EmptyQuantity: TaskUIState()
    object EmptyType: TaskUIState()
    object ConditionDate: TaskUIState()
    object EmptyAssignedTo: TaskUIState()

}