package com.xmobile.project1groupstudyappnew.model.state

import com.xmobile.project1groupstudyappnew.model.obj.Message

sealed class ChatUIState {
    object Idle : ChatUIState()
    object Loading : ChatUIState()
    data class Error(val message: String) : ChatUIState()
    object SuccessSendMessage : ChatUIState()
    data class SuccessListMessage(val messages: List<Message>) : ChatUIState()

}