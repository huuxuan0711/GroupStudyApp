package com.xmobile.project1groupstudyappnew.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project1groupstudyappnew.model.state.LoginAndRegisterUIState
import com.xmobile.project1groupstudyappnew.model.state.UserUIState
import com.xmobile.project1groupstudyappnew.repository.UserRepository
import com.xmobile.project1groupstudyappnew.utils.ValidateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
): ViewModel() {
    private val _userState = MutableStateFlow<UserUIState>(UserUIState.Idle)
    val userState: StateFlow<UserUIState> = _userState.asStateFlow()

    private val _loginAndRegisterState = MutableStateFlow<LoginAndRegisterUIState>(LoginAndRegisterUIState.Idle)
    val loginAndRegisterState: StateFlow<LoginAndRegisterUIState> = _loginAndRegisterState.asStateFlow()

    fun getUserInfo(uid: String) {
        viewModelScope.launch {
            try {
                _userState.value = UserUIState.Loading
                val result = userRepository.getUserInfo(uid)
                result.onSuccess { user ->
                    _userState.value = UserUIState.SuccessGetInfo(user)
                    }.onFailure { e ->
                    _userState.value = UserUIState.Error(e.message ?: "Không thể tải thông tin người dùng")
                }
                } catch (e: Exception) {
                _userState.value = UserUIState.Error(e.message ?: "Không thể tải thông tin người dùng")
            }
        }
    }

    fun getMemberInfo(groupId: String, memberId: String) {
        viewModelScope.launch {
            try {
                _userState.value = UserUIState.Loading
                val result = userRepository.getMemberInfo(groupId, memberId)
                result.onSuccess { member ->
                    Log.d("CheckFlow", "getMemberInfo success: $member")
                    _userState.value = UserUIState.SuccessGetMemberInfo(member)
                    }.onFailure { e ->
                    Log.d("CheckFlow", "fail: ${e.message}")
                    _userState.value = UserUIState.Error(e.message ?: "Không thể tải thông tin thành viên")
                    }
            } catch (e: Exception) {
                _userState.value = UserUIState.Error(e.message ?: "Không thể tải thông tin thành viên")
            }
        }
    }

    fun listMember(groupId: String) {
        viewModelScope.launch {
            try {
                _userState.value = UserUIState.Loading
                val result = userRepository.listMember(groupId)
                result.onSuccess { members ->
                    _userState.value = UserUIState.SuccessListMember(members)
                    }.onFailure { e ->
                    _userState.value = UserUIState.Error(e.message ?: "Không thể tải danh sách thành viên")
                }
            } catch (e: Exception) {
                _userState.value = UserUIState.Error(e.message ?: "Không thể tải danh sách thành viên")
            }
        }
    }

    fun logout(uid: String, context: Context){
        viewModelScope.launch {
            try {
                _userState.value = UserUIState.Loading
                val result = userRepository.logout(uid, context)
                result.onSuccess {
                    _userState.value = UserUIState.SuccessLogout
                }.onFailure { e ->
                    _userState.value = UserUIState.Error(e.message ?: "Đăng xuất thất bại")
                }
            } catch (e: Exception) {
                _userState.value = UserUIState.Error(e.message ?: "Đăng xuất thất bại")
            }
        }
    }

    fun resetPassword(oldPassword: String ,newPassword: String) {
        viewModelScope.launch {
            try {
                _loginAndRegisterState.value = LoginAndRegisterUIState.Idle
                validatePassword(oldPassword, newPassword)
                if (_loginAndRegisterState.value != LoginAndRegisterUIState.Idle) {
                    return@launch
                }
                _loginAndRegisterState.value = LoginAndRegisterUIState.Loading
                val result = userRepository.resetPassword(oldPassword, newPassword)
                result.onSuccess {
                    _loginAndRegisterState.value = LoginAndRegisterUIState.SuccessResetPassword
                }.onFailure { e ->
                    _loginAndRegisterState.value = LoginAndRegisterUIState.Error(e.message ?: "Đặt lại mật khẩu thất bại")
                }
            }catch (e: Exception) {
                _loginAndRegisterState.value = LoginAndRegisterUIState.Error(e.message ?: "Đặt lại mật khẩu thất bại")
            }
        }
    }

    fun modifyInfo(userName: String, userDescription: String, avatar: String, uid: String) {
        viewModelScope.launch {
            try {
                _userState.value = UserUIState.Idle
                validateInfo(userName, userDescription)
                if (_userState.value != UserUIState.Idle) {
                    return@launch
                }
                _userState.value = UserUIState.Loading
                val result = userRepository.modifyInfo(userName, userDescription, avatar, uid)
                result.onSuccess {
                    _userState.value = UserUIState.SuccessGetInfo(it)
                }.onFailure { e ->
                    _userState.value = UserUIState.Error(e.message ?: "Cập nhật thông tin thất bại")
                }
            } catch (e: Exception) {
                _userState.value = UserUIState.Error(e.message ?: "Cập nhật thông tin thất bại")
            }
        }
    }

    private fun validateInfo(name: String, description: String) {
        if (!ValidateUtil.emptyCheckGroupName(name)) {
            _userState.value = UserUIState.EmptyName
        }else if (ValidateUtil.conditionCheckGroupName(name)) {
            _userState.value = UserUIState.ConditionName
        }else if (ValidateUtil.conditionCheckDescription(description)) {
            _userState.value = UserUIState.ConditionDescription
        }
    }

    private fun validatePassword(oldPassword: String, newPassword: String) {
        if (!ValidateUtil.emptyCheckPassword(oldPassword)) {
            _loginAndRegisterState.value = LoginAndRegisterUIState.EmptyPassword
        } else if (!ValidateUtil.conditionCheckPassword(newPassword)){
            _loginAndRegisterState.value = LoginAndRegisterUIState.ConditionPassword
        } else if (!ValidateUtil.emptyCheckPassword(newPassword)) {
            _loginAndRegisterState.value = LoginAndRegisterUIState.EmptyPassword
        }
    }
}