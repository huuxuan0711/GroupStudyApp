package com.xmobile.project1groupstudyappnew.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.xmobile.project1groupstudyappnew.model.state.LoginAndRegisterUIState
import com.xmobile.project1groupstudyappnew.repository.AuthRepository
import com.xmobile.project1groupstudyappnew.utils.ValidateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel(){
    private val _loginState = MutableStateFlow<LoginAndRegisterUIState>(LoginAndRegisterUIState.Idle)
    var loginState : StateFlow<LoginAndRegisterUIState> = _loginState.asStateFlow()

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                validate(email, password)
                if (_loginState.value != LoginAndRegisterUIState.Idle){
                        return@launch
                }
                _loginState.value = LoginAndRegisterUIState.Loading
                val result = authRepository.loginWithEmail(email, password)
                result.onSuccess { user ->
                    _loginState.value = LoginAndRegisterUIState.SuccessLogin(user)
                }.onFailure { e ->
                    _loginState.value = LoginAndRegisterUIState.Error(e.message ?: "Không tồn tại tài khoản")
                }
            }catch (e: Exception){
                _loginState.value = LoginAndRegisterUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }

    fun loginWithGoogle(context: Context) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginAndRegisterUIState.Loading
                val result = authRepository.loginWithGoogle(context)
                result.onSuccess { user ->
                    _loginState.value = LoginAndRegisterUIState.SuccessLogin(user)
                    authRepository.saveToDatabase(user)
                }.onFailure { e ->
                    _loginState.value = LoginAndRegisterUIState.Error(e.message ?: "Không tồn tại tài khoản")
                }
            }catch (e: Exception){
                _loginState.value = LoginAndRegisterUIState.Error(e.message ?: "Dữ liệu không hợp lệ")

            }
        }
    }

    fun loginWithFacebook(user: FirebaseUser) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginAndRegisterUIState.Loading
                val result = authRepository.saveToDatabase(user)
                result.onSuccess { user ->
                    _loginState.value = LoginAndRegisterUIState.Idle
                }.onFailure { e ->
                    _loginState.value = LoginAndRegisterUIState.Error(e.message ?: "Không tồn tại tài khoản")
                }
            }catch (e: Exception){
                _loginState.value = LoginAndRegisterUIState.Error(e.message ?: "Dữ liệu không hợp lệ")

            }
        }
    }

    fun loginWithMicrosoft(activity: Activity) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginAndRegisterUIState.Loading
                val result = authRepository.loginWithMicrosoft(activity)
                result.onSuccess { user ->
                    _loginState.value = LoginAndRegisterUIState.SuccessLogin(user)
                    authRepository.saveToDatabase(user)
                }.onFailure { e ->
                    _loginState.value = LoginAndRegisterUIState.Error(e.message ?: "Không tồn tại tài khoản")
                }
            }catch (e: Exception){
                _loginState.value = LoginAndRegisterUIState.Error(e.message ?: "Dữ liệu không hợp lệ")

            }
        }
    }

    private fun validate(email: String, password: String) {
        if (!ValidateUtil.emptyCheckEmail(email)) {
            _loginState.value = LoginAndRegisterUIState.EmptyEmail
        }else if (!ValidateUtil.emptyCheckPassword(password)) {
            _loginState.value = LoginAndRegisterUIState.EmptyPassword
        }else if (!ValidateUtil.formatCheck(email)) {
            _loginState.value = LoginAndRegisterUIState.FormatEmail
        }
    }
}