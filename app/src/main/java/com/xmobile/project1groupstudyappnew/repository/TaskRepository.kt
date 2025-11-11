package com.xmobile.project1groupstudyappnew.repository

import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.GroupProgress
import com.xmobile.project1groupstudyappnew.model.obj.member.MemberProgress
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.task.TaskInput
import com.xmobile.project1groupstudyappnew.model.obj.user.User

interface TaskRepository {
    suspend fun listTask(groupId: String): Result<List<Task>>
    suspend fun listTaskAllOfUser(userId: String): Result<List<Task>>
    suspend fun listTaskInGroupOfUser(groupId: String, userId: String): Result<List<Task>>
    suspend fun mapTaskWithUser(user: User): Result<Map<Int, List<Task>>>
    suspend fun listFileWithTask(task: Task): Result<List<File>>
    suspend fun getProgressGroup(time: Int, groupId: String): Result<GroupProgress>
    suspend fun getProgressMember(time: Int, groupId: String): Result<List<MemberProgress>>
    suspend fun createTask(input: TaskInput): Result<Task>
    suspend fun updateTask(
        id: String,
        input: TaskInput
    ): Result<Task>
    suspend fun deleteTask(task: Task): Result<Task>
    suspend fun getTaskFromId(id: String): Result<Task>
    suspend fun updateStatus(task: Task, status: Int, userId: String): Result<Int>
}