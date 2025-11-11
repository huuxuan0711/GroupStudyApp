package com.xmobile.project1groupstudyappnew.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project1groupstudyappnew.model.state.NotificationUIState
import com.xmobile.project1groupstudyappnew.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
): ViewModel() {
    private val _notificationState = MutableStateFlow<NotificationUIState>(NotificationUIState.Idle)
    var notificationState: StateFlow<NotificationUIState> = _notificationState.asStateFlow()

    fun listNotification(userId: String){
        viewModelScope.launch {
            try {
                _notificationState.value = NotificationUIState.Loading
                val result = notificationRepository.listNotification(userId)
                result.onSuccess {
                    _notificationState.value = NotificationUIState.SuccessList(it)
                }.onFailure { e ->
                    _notificationState.value =
                        NotificationUIState.Error(e.message ?: "Không thể lấy danh sách thông báo")
                }
            }catch (e: Exception) {
                _notificationState.value = NotificationUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }

    fun deleteNotification(notiId: String){
        viewModelScope.launch {
            try {
                _notificationState.value = NotificationUIState.Loading
                val result = notificationRepository.deleteNotification(notiId)
                result.onSuccess {
                    _notificationState.value = NotificationUIState.SuccessDelete(it)
                }.onFailure { e ->
                    _notificationState.value =
                        NotificationUIState.Error(e.message ?: "Không thể xóa thông báo")
                }
            }catch (e: Exception) {
                _notificationState.value = NotificationUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }

    fun readNotification(notiId: String){
        viewModelScope.launch {
            try {
                _notificationState.value = NotificationUIState.Loading
                val result = notificationRepository.readNotification(notiId)
                result.onSuccess {
                    _notificationState.value = NotificationUIState.SuccessRead(it)
                }.onFailure { e ->
                    _notificationState.value =
                        NotificationUIState.Error(e.message ?: "Không thể đọc thông báo")
                    }
            }catch (e: Exception) {
                _notificationState.value = NotificationUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }
}