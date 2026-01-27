package com.xmobile.project1groupstudyappnew.helper

import android.content.Intent

object PendingNavigation {

    private var target: Target? = null
    private var notificationId: String? = null

    fun toGroup(groupId: String, notificationId: String?) {
        target = Target.Group(groupId)
        this.notificationId = notificationId
    }

    fun toTask(taskId: String, notificationId: String?) {
        target = Target.Task(taskId)
        this.notificationId = notificationId
    }

    fun toInvite(inviteCode: String, notificationId: String?) {
        target = Target.Invite(inviteCode)
        this.notificationId = notificationId
    }

    fun applyTo(intent: Intent) {
        when (val t = target) {
            is Target.Group -> intent.putExtra("groupId", t.groupId)
            is Target.Task -> intent.putExtra("taskId", t.taskId)
            is Target.Invite -> intent.putExtra("inviteCode", t.inviteCode)
            null -> {}
        }

        notificationId?.let {
            intent.putExtra("notificationId", it)
        }

        clear()
    }

    private fun clear() {
        target = null
        notificationId = null
    }

    sealed class Target {
        data class Group(val groupId: String) : Target()
        data class Task(val taskId: String) : Target()
        data class Invite(val inviteCode: String) : Target()
    }
}
