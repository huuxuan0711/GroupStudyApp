package com.xmobile.project1groupstudyappnew.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.state.GroupUIState
import com.xmobile.project1groupstudyappnew.repository.GroupRepository
import com.xmobile.project1groupstudyappnew.utils.ValidateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository
): ViewModel() {

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _groupState = MutableStateFlow<GroupUIState>(GroupUIState.Idle)
    val groupState: StateFlow<GroupUIState> = _groupState.asStateFlow()

    fun setGroup(group: Group) {
        viewModelScope.launch {
            try {
                _group.value = group
            }catch (e: Exception) {
                _groupState.value = GroupUIState.Error(e.message ?: "Không thể lấy thông tin nhóm")
            }
        }
    }

    fun getGroupFromId(groupId: String){
        viewModelScope.launch {
            try {
                _groupState.value = GroupUIState.Loading
                val result = groupRepository.getGroupFromId(groupId)
                result.onSuccess {
                    _groupState.value = GroupUIState.SuccessGetGroup(it)
                }.onFailure { e ->
                    _groupState.value = GroupUIState.Error(e.message ?: "Không thể lấy thông tin nhóm")
                }
            }catch (e: Exception) {
                _groupState.value = GroupUIState.Error(e.message ?: "Không thể lấy thông tin nhóm")
            }
        }
    }

    fun checkRole(uid: String, groupId: String) {
        viewModelScope.launch {
            try {
                _groupState.value = GroupUIState.Loading
                val result = groupRepository.checkRole(uid, groupId)
                result.onSuccess {
                    _groupState.value = GroupUIState.Role(it)
                }.onFailure { e ->
                    _groupState.value = GroupUIState.Error(e.message ?: "Không thể kiểm tra role")
                }
            }catch (e: Exception) {
                _groupState.value = GroupUIState.Error(e.message ?: "Không thể kiểm tra role")
            }
        }
    }

    fun authorizeMember(groupId: String, userId: String, role: Int) {
        viewModelScope.launch {
            try {
                _groupState.value = GroupUIState.Loading
                val result = groupRepository.authorizeMember(groupId, userId, role)
                result.onSuccess {
                    _groupState.value = GroupUIState.SuccessAuthorize
                }.onFailure { e ->
                    _groupState.value = GroupUIState.Error(e.message ?: "Không thể bổ nhiệm thành viên")
                }
            }catch (e: Exception) {
                _groupState.value = GroupUIState.Error(e.message ?: "Không thể bổ nhiệm thành viên")
            }
        }
    }

    fun deleteMember(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                _groupState.value = GroupUIState.Loading
                val result = groupRepository.deleteMember(groupId, userId)
                result.onSuccess {
                    _groupState.value = GroupUIState.SuccessDeleteMember(userId)
                    }.onFailure { e ->
                    _groupState.value = GroupUIState.Error(e.message ?: "Không thể xóa thành viên")
                }
            }catch (e: Exception) {
                _groupState.value = GroupUIState.Error(e.message ?: "Không thể xóa thành viên")
            }
        }
    }

    fun inviteMember(groupId: String, invitee: String, inviter: String?) {
        viewModelScope.launch {
            try {
                _groupState.value = GroupUIState.Loading
                val result = groupRepository.inviteMember(groupId, invitee, inviter)
                result.onSuccess {
                    _groupState.value = GroupUIState.SuccessInviteMember(inviter!!)
                }.onFailure { e ->
                    _groupState.value = GroupUIState.Error(e.message ?: "Không thể mời thành viên")
                }
            }catch (e: Exception) {
                _groupState.value = GroupUIState.Error(e.message ?: "Không thể mời thành viên")
            }
        }
    }

    fun modifyInfo(name: String, description: String, avatar: String, groupId: String) {
        viewModelScope.launch {
            try {
                _groupState.value = GroupUIState.Idle
                validateInfo(name, description)
                if (_groupState.value != GroupUIState.Idle) {
                    return@launch
                }
                _groupState.value = GroupUIState.Loading
                val result = groupRepository.modifyInfo(name, description, avatar, groupId)
                result.onSuccess {
                    _groupState.value = GroupUIState.SuccessModify(name, description)
                }.onFailure { e ->
                    _groupState.value = GroupUIState.Error(e.message ?: "Không thể sửa thông tin nhóm")
                }
            } catch (e: Exception) {
                _groupState.value = GroupUIState.Error(e.message ?: "Không thể sửa thông tin nhóm")
            }
        }
    }

    fun uploadAvatarToCloudinary(imageUri: Uri, id: String) {
        viewModelScope.launch {
            try {
                _groupState.value = GroupUIState.Loading
                val result = groupRepository.uploadAvatarToCloudinary(imageUri, id) // id = groupId hoặc userId
                result.onSuccess {
                    _groupState.value = GroupUIState.SuccessUpload(it)
                }.onFailure { e ->
                    _groupState.value = GroupUIState.Error(e.message ?: "Không thể tải ảnh đại diện")
                }

            }catch (e: Exception){
                _groupState.value = GroupUIState.Error(e.message ?: "Không thể tải ảnh đại diện")
            }
        }
    }

    fun leaveGroup(uid: String, groupId: String){
        viewModelScope.launch {
            try {
                _groupState.value = GroupUIState.Loading
                val result = groupRepository.leaveGroup(uid, groupId)
                result.onSuccess {
                    _groupState.value = GroupUIState.SuccessLeave
                }.onFailure { e ->
                    _groupState.value = GroupUIState.Error(e.message ?: "Không thể rời nhóm")
                }
            } catch (e: Exception) {
                _groupState.value = GroupUIState.Error(e.message ?: "Không thể rời nhóm")
            }
        }
    }

    private fun validateInfo(name: String, description: String) {
        if (!ValidateUtil.emptyCheckGroupName(name)) {
            _groupState.value = GroupUIState.EmptyName
        }else if (ValidateUtil.conditionCheckGroupName(name)) {
            _groupState.value = GroupUIState.ConditionName
        }else if (ValidateUtil.conditionCheckDescription(description)) {
            _groupState.value = GroupUIState.ConditionDescription
        }
    }
}