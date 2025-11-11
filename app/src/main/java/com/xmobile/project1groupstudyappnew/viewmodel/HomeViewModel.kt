package com.xmobile.project1groupstudyappnew.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.state.HomeUIState
import com.xmobile.project1groupstudyappnew.repository.GroupRepository
import com.xmobile.project1groupstudyappnew.utils.ValidateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _homeState = MutableStateFlow<HomeUIState>(HomeUIState.Idle)
    val homeState: StateFlow<HomeUIState> = _homeState.asStateFlow()

    private val _groupUpdate = MutableStateFlow<Map<String, String>>(emptyMap())
    val groupUpdate: StateFlow<Map<String, String>> = _groupUpdate.asStateFlow()

    private val _groupInvite = MutableStateFlow("")
    val groupInvite: StateFlow<String> = _groupInvite.asStateFlow()

    private val _latestMessageMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val latestMessageMap: StateFlow<Map<String, String>> = _latestMessageMap.asStateFlow()

    private val _lastSeenMessageMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val lastSeenMessageMap: StateFlow<Map<String, String>> = _lastSeenMessageMap.asStateFlow()


    private val listenedGroups = mutableSetOf<String>()

    fun listenGroupUpdates(groupId: String) {
        if (listenedGroups.contains(groupId)) return
        listenedGroups.add(groupId)
        viewModelScope.launch {
            groupRepository.listenGroupUpdates(groupId).collect { message ->
                notifyGroupUpdate(groupId, message)
            }
        }
    }

    fun notifyGroupUpdate(groupId: String, message: String) {
        val map = _groupUpdate.value.toMutableMap()
        map[groupId] = message
        _groupUpdate.value = map
    }

    fun clearGroupNotification(groupId: String) {
        val map = _groupUpdate.value.toMutableMap()
        map.remove(groupId)
        _groupUpdate.value = map
    }

    fun setLastSeenMap(map: Map<String, String>) {
        _lastSeenMessageMap.value = map
    }

    fun updateLatestMessage(groupId: String, messageId: String) {
        val lastSeenId = _lastSeenMessageMap.value[groupId]
        val latest = _latestMessageMap.value.toMutableMap()

        // Chỉ cập nhật nếu message mới hơn lastSeen
        if (lastSeenId != messageId) {
            latest[groupId] = messageId
            _latestMessageMap.value = latest
        }
    }

    fun markGroupAsRead(groupId: String) {
        val latestId = _latestMessageMap.value[groupId] ?: return
        val map = _lastSeenMessageMap.value.toMutableMap()
        map[groupId] = latestId
        _lastSeenMessageMap.value = map

        // Clear notification badge
        clearGroupNotification(groupId)

        // Cập nhật lastSeenMessageId lên database
        viewModelScope.launch {
            groupRepository.updateLastSeenMessageId(groupId, latestId)
        }
    }

    fun listGroup(uid: String) {
        viewModelScope.launch {
            try {
                _homeState.value = HomeUIState.Loading
                val result = groupRepository.listGroup(uid)
                result.onSuccess { groups ->
                    // Khởi tạo lastSeenMessageMap từ group
                    val lastSeenMap = groups.associate { it.groupId to (it.lastSeenMessageId ?: "") }
                    setLastSeenMap(lastSeenMap)

                    _homeState.value = HomeUIState.SuccessList(groups)

                    // Start listening for each group
                    groups.forEach { group -> listenGroupUpdates(group.groupId) }
                }.onFailure { e ->
                    _homeState.value = HomeUIState.Error(e.message ?: "Không thể lấy danh sách nhóm")
                }
            } catch (e: Exception) {
                _homeState.value = HomeUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }


    fun createGroup(
        name: String,
        description: String,
        type: Int,
        adminId: String
    ) {
        viewModelScope.launch {
            try {
                _homeState.value = HomeUIState.Idle
                validateCreate(name, description)
                if (_homeState.value != HomeUIState.Idle) return@launch

                _homeState.value = HomeUIState.Loading

                val result = groupRepository.addGroup(name,description,type, adminId)
                result.onSuccess {
                    _homeState.value = HomeUIState.SuccessCreate(it)
                }.onFailure { e ->
                    _homeState.value = HomeUIState.Error(e.message ?: "Không thể tạo nhóm")
                }
            } catch (e: Exception) {
                _homeState.value = HomeUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }

    fun checkGroupExist(uid: String, inviteCode: String) {
        viewModelScope.launch {
            try {
                _homeState.value = HomeUIState.Idle
                validateJoin(inviteCode)
                if (_homeState.value != HomeUIState.Idle) return@launch

                _homeState.value = HomeUIState.Loading
                val result = groupRepository.checkGroupExist(uid, inviteCode)
                result.onSuccess {
                    _homeState.value = HomeUIState.SuccessCheckExist(it)
                }.onFailure { e ->
                    _homeState.value = HomeUIState.Error(e.message ?: "Không thể kiểm tra nhóm")
                }
            } catch (e: Exception) {
                _homeState.value = HomeUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }

    fun joinGroup(uid: String, group: Group) {
        viewModelScope.launch {
            try {
                _homeState.value = HomeUIState.Loading
                val result = groupRepository.joinGroup(uid, group)
                result.onSuccess {
                    _homeState.value = HomeUIState.SuccessJoin(it)
                    // Start listening for new messages in this group
                    listenGroupUpdates(it.groupId)
                }.onFailure { e ->
                    _homeState.value = HomeUIState.Error(e.message ?: "Không thể tham gia nhóm")
                }
            } catch (e: Exception) {
                _homeState.value = HomeUIState.Error(e.message ?: "Dữ liệu không hợp lệ")
            }
        }
    }

    fun setGroupInvite(inviteCode: String) {
        viewModelScope.launch { _groupInvite.value = inviteCode }
    }

    private fun validateCreate(name: String, description: String) {
        when {
            !ValidateUtil.emptyCheckGroupName(name) -> _homeState.value = HomeUIState.EmptyName
            ValidateUtil.conditionCheckGroupName(name) -> _homeState.value = HomeUIState.ConditionName
            ValidateUtil.conditionCheckDescription(description) -> _homeState.value = HomeUIState.ConditionDescription
        }
    }

    private fun validateJoin(groupId: String) {
        if (!ValidateUtil.emptyCheckGroupId(groupId)) {
            _homeState.value = HomeUIState.EmptyGroupId
        }
    }
}
