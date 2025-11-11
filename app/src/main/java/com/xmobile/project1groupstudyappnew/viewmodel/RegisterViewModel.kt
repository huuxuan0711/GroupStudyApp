package com.xmobile.project1groupstudyappnew.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.xmobile.project1groupstudyappnew.model.state.LoginAndRegisterUIState
import com.xmobile.project1groupstudyappnew.repository.AuthRepository
import com.xmobile.project1groupstudyappnew.utils.ValidateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _registerResult = MutableStateFlow<LoginAndRegisterUIState>(LoginAndRegisterUIState.Idle)
    val registerResult: StateFlow<LoginAndRegisterUIState> = _registerResult.asStateFlow()

    fun registerWithEmail(email: String, password: String, confirmPassword: String, userName: String){
        viewModelScope.launch {
            try {
                validate(email, password, confirmPassword, userName)
                if (_registerResult.value != LoginAndRegisterUIState.Idle){
                    return@launch
                }
                _registerResult.value = LoginAndRegisterUIState.Loading
                val result = authRepository.registerWithEmail(email, password, confirmPassword, userName)
                result.onSuccess { user ->
                    _registerResult.value = LoginAndRegisterUIState.SuccessRegister(user)
                    authRepository.saveToDatabase(user)
                }.onFailure { e ->
                    _registerResult.value = LoginAndRegisterUIState.Error(e.message ?: "Đăng ký thất bại")
                }
            }catch (e: Exception){
                _registerResult.value = LoginAndRegisterUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }

    fun registerWithGoogle(context: Context) {
        viewModelScope.launch {
            try {
                _registerResult.value = LoginAndRegisterUIState.Loading
                val result = authRepository.loginWithGoogle(context)
                result.onSuccess { user ->
                    _registerResult.value = LoginAndRegisterUIState.SuccessLogin(user)
                    authRepository.saveToDatabase(user)
                }.onFailure { e ->
                    _registerResult.value = LoginAndRegisterUIState.Error(e.message ?: "Không tồn tại tài khoản")
                }
            }catch (e: Exception){
                _registerResult.value = LoginAndRegisterUIState.Error(e.message ?: "Dữ liệu không hợp lệ")

            }
        }
    }

    fun registerWithFacebook(user: FirebaseUser) {
        viewModelScope.launch {
            try {
                _registerResult.value = LoginAndRegisterUIState.Loading
                val result = authRepository.saveToDatabase(user)
                result.onSuccess { user ->
                    _registerResult.value = LoginAndRegisterUIState.Idle
                }.onFailure { e ->
                    _registerResult.value = LoginAndRegisterUIState.Error(e.message ?: "Không tồn tại tài khoản")
                }
            }catch (e: Exception){
                _registerResult.value = LoginAndRegisterUIState.Error(e.message ?: "Dữ liệu không hợp lệ")

            }
        }
    }

    fun registerWithMicrosoft(activity: Activity) {
        viewModelScope.launch {
            try {
                _registerResult.value = LoginAndRegisterUIState.Loading
                val result = authRepository.loginWithMicrosoft(activity)
                result.onSuccess { user ->
                    _registerResult.value = LoginAndRegisterUIState.SuccessLogin(user)
                    authRepository.saveToDatabase(user)
                }.onFailure { e ->
                    _registerResult.value = LoginAndRegisterUIState.Error(e.message ?: "Không tồn tại tài khoản")
                }
            }catch (e: Exception){
                _registerResult.value = LoginAndRegisterUIState.Error(e.message ?: "Dữ liệu không hợp lệ")

            }
        }
    }

    fun validate(email: String, password: String, confirmPassword: String, userName: String){
        if (!ValidateUtil.emptyCheckEmail(email)) {
            _registerResult.value = LoginAndRegisterUIState.EmptyEmail
        }else if (!ValidateUtil.emptyCheckPassword(password)) {
            _registerResult.value = LoginAndRegisterUIState.EmptyPassword
        }else if (!ValidateUtil.emptyCheckUserName(userName)) {
            _registerResult.value = LoginAndRegisterUIState.EmptyUserName
        }else if (!ValidateUtil.formatCheck(email)) {
            _registerResult.value = LoginAndRegisterUIState.FormatEmail
        }else if (!ValidateUtil.conditionCheckPassword(password)) {
            _registerResult.value = LoginAndRegisterUIState.ConditionPassword
        }else if (!ValidateUtil.conditionCheckMatching(password, confirmPassword)) {
            _registerResult.value = LoginAndRegisterUIState.MatchingPassword
        }else if (!ValidateUtil.conditionCheckUserName(userName)) {
            _registerResult.value = LoginAndRegisterUIState.ConditionUserName
        }
    }
}