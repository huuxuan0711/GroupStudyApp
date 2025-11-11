package com.xmobile.project1groupstudyappnew.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.view.activity.GroupActivity
import com.xmobile.project1groupstudyappnew.view.activity.MainActivity
import com.xmobile.project1groupstudyappnew.view.activity.TaskDetailActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        saveTokenToDatabase(token)
    }

    private fun saveTokenToDatabase(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseDatabase.getInstance().getReference("UserTokens")
        db.child(user.uid).setValue(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val type = data["type"] ?: "general"
        val notificationId = data["id"] ?: data["notificationId"]
        val groupId = data["groupId"]
        val taskId = data["taskId"]
        val inviteCode = data["inviteCode"]
        val message = data["message"] ?: data["body"] ?: ""

        // 🔹 Hiển thị notification ngoài foreground
        val title = remoteMessage.notification?.title ?: when (type) {
            "task" -> "Cập nhật nhiệm vụ"
            "message" -> "Tin nhắn mới"
            "member" -> "Thành viên mới"
            "file" -> "Tệp mới"
            "invite" -> "Lời mời tham gia nhóm"
            "deadline" -> "Sắp đến hạn nhiệm vụ"
            else -> "Thông báo"
        }

        sendNotification(title, message, type, notificationId, groupId, taskId, inviteCode)
    }

    private fun sendNotification(
        title: String,
        body: String,
        type: String,
        notificationId: String? = null,
        groupId: String? = null,
        taskId: String? = null,
        inviteCode: String? = null
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "group_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Group Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val targetIntent = when (type) {
            "invite" -> MainActivity::class.java
            "deadline", "task" -> TaskDetailActivity::class.java
            else -> GroupActivity::class.java
        }.let { cls ->
            Intent(this, cls).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                notificationId?.let { putExtra("notificationId", it) }
                groupId?.let { putExtra("groupId", it) }
                taskId?.let { putExtra("taskId", it) }
                inviteCode?.let { putExtra("inviteCode", it) }
            }
        }

        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_noti)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        notificationManager.notify(requestCode, notification)
    }
}
