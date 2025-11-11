package com.xmobile.project1groupstudyappnew.model.state

import com.xmobile.project1groupstudyappnew.model.obj.Notification

sealed class NotificationUIState {
    object Idle : NotificationUIState()
    object Loading : NotificationUIState()
    data class SuccessList(val notifications: List<Notification>) : NotificationUIState()
    data class SuccessDelete(val notification: Notification) : NotificationUIState()
    data class SuccessRead(val notification: Notification) : NotificationUIState()
    data class Error(val message: String) : NotificationUIState()

}