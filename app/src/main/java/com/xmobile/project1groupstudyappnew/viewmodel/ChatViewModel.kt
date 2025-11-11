package com.xmobile.project1groupstudyappnew.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.Message
import com.xmobile.project1groupstudyappnew.model.state.ChatUIState
import com.xmobile.project1groupstudyappnew.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
): ViewModel(){
    private val _chatState = MutableStateFlow<ChatUIState>(ChatUIState.Idle)
    val chatState: StateFlow<ChatUIState> = _chatState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    fun listenMessages(groupId: String) {
        chatRepository.observeMessages(groupId) { newMsg, isDeleted ->
            if (isDeleted) {
                _messages.value = _messages.value.filter { it.id != newMsg.id }
            } else _messages.value = _messages.value + newMsg
        }
    }

    fun sendMessage(groupId: String, senderId: String, memberName: String, avatar: String, text: String){
        viewModelScope.launch {
            _chatState.value = ChatUIState.Loading
            val result = chatRepository.sendMessage(groupId, senderId, memberName, avatar, text)
            result.onSuccess {
                _chatState.value = ChatUIState.SuccessSendMessage
            }
            result.onFailure {
                _chatState.value = ChatUIState.Error(it.message.toString())
            }
        }
    }

    fun uploadFile(groupId: String, senderId: String, memberName: String, avatar: String, file: File?){
        viewModelScope.launch {
            _chatState.value = ChatUIState.Loading
            val result = chatRepository.uploadFile(groupId, senderId, memberName, avatar, file)
            result.onSuccess {
                _chatState.value = ChatUIState.SuccessSendMessage
            }
            result.onFailure {
                _chatState.value = ChatUIState.Error(it.message.toString())
            }
        }
    }

    fun listMessage(groupId: String) {
        viewModelScope.launch {
            _chatState.value = ChatUIState.Loading
            val result = chatRepository.listMessage(groupId)
            result.onSuccess {
                _chatState.value = ChatUIState.SuccessListMessage(it)
            }
            result.onFailure {
                _chatState.value = ChatUIState.Error(it.message.toString())
            }
        }
    }
}