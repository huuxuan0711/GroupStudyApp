package com.xmobile.project1groupstudyappnew.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project1groupstudyappnew.model.state.PasswordUIState
import com.xmobile.project1groupstudyappnew.repository.PasswordRepository
import com.xmobile.project1groupstudyappnew.utils.ValidateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgetPasswordViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository
): ViewModel() {
    private val _passwordState = MutableStateFlow<PasswordUIState>(PasswordUIState.Idle)
    val passwordState: StateFlow<PasswordUIState> = _passwordState.asStateFlow()

    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    fun sendEmail(email: String) {
        viewModelScope.launch {
            try {
                validate(email)
                if (_passwordState.value != PasswordUIState.Idle) {
                    return@launch
                }
                _passwordState.value = PasswordUIState.Loading
                val result = passwordRepository.sendEmail(email)
                result.onSuccess { email ->
                    _passwordState.value = PasswordUIState.Success(email)
                }
                result.onFailure { e ->
                    _passwordState.value = PasswordUIState.Error(e.message ?: "Lỗi không xác định")
                }
            }catch (e: Exception) {
                _passwordState.value = PasswordUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }

    private fun validate(email: String) {
        if (!ValidateUtil.emptyCheckEmail(email)) {
            _passwordState.value = PasswordUIState.EmptyEmail
        }else if (!ValidateUtil.formatCheck(email)) {
            _passwordState.value = PasswordUIState.FormatEmail
        }
    }

    fun startCountdown(seconds: Int) {
        viewModelScope.launch {
            _countdown.value = seconds
            for (i in seconds downTo 1) {
                delay(1000)
                _countdown.value = i - 1
            }
        }
    }
}