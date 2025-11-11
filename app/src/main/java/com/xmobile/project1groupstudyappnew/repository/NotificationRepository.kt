package com.xmobile.project1groupstudyappnew.repository

import com.xmobile.project1groupstudyappnew.model.obj.Notification

interface NotificationRepository {
    suspend fun listNotification(userId: String): Result<List<Notification>>
    suspend fun deleteNotification(notiId: String): Result<Notification>
    suspend fun readNotification(notiId: String): Result<Notification>
}