package com.xmobile.project1groupstudyappnew.repository

import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.user.User

interface SearchRepository {
    suspend fun searchUser(query: String): Result<List<User>>
    suspend fun searchGroup(query: String): Result<List<Group>>
    suspend fun searchFile(query: String): Result<List<File>>
    suspend fun searchTask(query: String): Result<List<Task>>
}