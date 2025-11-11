package com.xmobile.project1groupstudyappnew.repository

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.Message
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
): ChatRepository{

    override suspend fun sendMessage(
        groupId: String,
        senderId: String,
        memberName: String,
        avatar: String,
        text: String
    ): Result<Boolean> {
        return try {
            val db = firebaseDatabase.reference
            val messageId = db.child("messages").push().key ?: return Result.failure(Exception("Không thể tạo ID cho tin nhắn"))
            val message = Message(
                id = messageId,
                groupId = groupId,
                senderId = senderId,
                memberName = memberName,
                avatar = avatar,
                text = text,
                timestamp = System.currentTimeMillis())
            db.child("messages").child(messageId).setValue(message)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(
        groupId: String,
        senderId: String,
        memberName: String,
        avatar: String,
        file: File?
    ): Result<Boolean> {
        return try {
            val db = firebaseDatabase.reference
            val messageId = db.child("messages").push().key ?: return Result.failure(Exception("Không thể tạo ID cho tin nhắn"))
            val message = Message(
                id = messageId,
                groupId = groupId,
                senderId = senderId,
                memberName = memberName,
                avatar = avatar,
                file = file,
                timestamp = System.currentTimeMillis())
            db.child("messages").child(messageId).setValue(message)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeMessages(groupId: String, onNewMessage: (Message, Boolean) -> Unit) {
        val isDeleted = false
        val ref = firebaseDatabase.reference
            .child("messages")
            .orderByChild("groupId")
            .equalTo(groupId)

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(Message::class.java)?.let { onNewMessage(it, isDeleted) }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.getValue(Message::class.java)?.let { onNewMessage(it, !isDeleted) }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override suspend fun listMessage(groupId: String): Result<List<Message>> {
        return try {
            val db = firebaseDatabase.reference
            val snapshot = db.child("messages").orderByChild("groupId").equalTo(groupId).get().await()
            val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}