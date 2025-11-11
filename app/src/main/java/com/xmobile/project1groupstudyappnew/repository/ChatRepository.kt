package com.xmobile.project1groupstudyappnew.repository

import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.Message

interface ChatRepository {
    suspend fun sendMessage(groupId: String, senderId: String, memberName: String, avatar: String, text: String): Result<Boolean>
    suspend fun uploadFile(groupId: String, senderId: String, memberName: String, avatar: String, file: File?): Result<Boolean>
    fun observeMessages(groupId: String, onNewMessage: (Message, Boolean) -> Unit)
    suspend fun listMessage(groupId: String): Result<List<Message>>
}