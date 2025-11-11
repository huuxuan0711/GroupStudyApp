package com.xmobile.project1groupstudyappnew.repository

import com.google.firebase.database.FirebaseDatabase
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import java.text.Normalizer
import java.util.regex.Pattern

class SearchRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) : SearchRepository {

    // Hàm loại bỏ dấu và chuẩn hóa chuỗi
    private fun normalize(text: String): String {
        val temp = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
            .matcher(temp)
            .replaceAll("")
    }

    override suspend fun searchUser(query: String): Result<List<User>> {
        return try {
            val snapshot = firebaseDatabase.getReference("users").get().await()
            val normalizedQuery = normalize(query)
            val list = snapshot.children.mapNotNull {
                it.getValue(User::class.java)
            }.filter { user ->
                normalize(user.name).contains(normalizedQuery) || normalize(user.inviteCode).contains(normalizedQuery)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchGroup(query: String): Result<List<Group>> {
        return try {
            val snapshot = firebaseDatabase.getReference("groups").get().await()
            val normalizedQuery = normalize(query)
            val list = snapshot.children.mapNotNull { it.getValue(Group::class.java) }
                .filter { group ->
                    normalize(group.name).contains(normalizedQuery)
                }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchFile(query: String): Result<List<File>> {
        return try {
            val snapshot = firebaseDatabase.getReference("files").get().await()
            val normalizedQuery = normalize(query)
            val list = snapshot.children.mapNotNull { it.getValue(File::class.java) }
                .filter { file ->
                    normalize(file.name).contains(normalizedQuery)
                }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchTask(query: String): Result<List<Task>> {
        return try {
            val snapshot = firebaseDatabase.getReference("tasks").get().await()
            val normalizedQuery = normalize(query)
            val list = snapshot.children.mapNotNull { it.getValue(Task::class.java) }
                .filter { task ->
                    normalize(task.title).contains(normalizedQuery)
                }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

