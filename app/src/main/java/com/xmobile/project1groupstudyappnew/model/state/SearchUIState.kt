package com.xmobile.project1groupstudyappnew.model.state

import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.user.User

sealed class SearchUIState {
    object Idle : SearchUIState()
    object Loading : SearchUIState()
    data class SuccessSearchUser(val users: List<User>) : SearchUIState()
    data class SuccessSearchGroup(val groups: List<Group>) : SearchUIState()
    data class SuccessSearchFile(val files: List<File>) : SearchUIState()
    data class SuccessSearchTask(val tasks: List<Task>) : SearchUIState()
    data class Error(val message: String) : SearchUIState()
}