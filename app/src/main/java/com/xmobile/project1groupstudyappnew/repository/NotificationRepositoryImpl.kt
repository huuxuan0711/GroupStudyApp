package com.xmobile.project1groupstudyappnew.repository

import com.google.firebase.database.FirebaseDatabase
import com.xmobile.project1groupstudyappnew.model.obj.Notification
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) : NotificationRepository {

    override suspend fun listNotification(userId: String): Result<List<Notification>> {
        return try {
            // Truy cập nhánh userId
            val notificationsRef = firebaseDatabase.reference
                .child("notifications")
                .child(userId)

            val snapshot = notificationsRef.get().await()
            val notificationList = mutableListOf<Notification>()

            for (notificationSnapshot in snapshot.children) {
                val notification = notificationSnapshot.getValue(Notification::class.java)
                notification?.let { notificationList.add(it) }
            }

            Result.success(notificationList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notiId: String): Result<Notification> {
        return try {
            val notificationRef = firebaseDatabase.reference.child("notifications").child(notiId)
            notificationRef.removeValue().await()
            val notification = notificationRef.get().await().getValue(Notification::class.java)
            Result.success(notification!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readNotification(notiId: String): Result<Notification> {
        return try {
            val notificationRef = firebaseDatabase.reference.child("notifications").child(notiId)
            notificationRef.child("read").setValue(true).await()
            val notification = notificationRef.get().await().getValue(Notification::class.java)
            Result.success(notification!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}