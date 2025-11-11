package com.xmobile.project1groupstudyappnew.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.task.TaskInput
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import com.xmobile.project1groupstudyappnew.model.state.TaskUIState
import com.xmobile.project1groupstudyappnew.repository.TaskRepository
import com.xmobile.project1groupstudyappnew.utils.ValidateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
): ViewModel() {
    private val _taskState = MutableStateFlow<TaskUIState>(TaskUIState.Idle)
    val taskState: StateFlow<TaskUIState> = _taskState.asStateFlow()

    private val _countByDay = MutableStateFlow<Map<String, Int>>(emptyMap())
    val countByDay = _countByDay.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks = _tasks.asStateFlow()

    fun listTask(groupId: String){
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Loading
                val result = taskRepository.listTask(groupId)
                result.onSuccess {
                    _taskState.value = TaskUIState.SuccessList(it)
                }.onFailure { e ->
                    _taskState.value = TaskUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    fun getTaskFromId(id: String) {
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Loading
                val result = taskRepository.getTaskFromId(id)
                result.onSuccess {
                    _taskState.value = TaskUIState.SuccessGetTask(it)
                }.onFailure { e ->
                    _taskState.value = TaskUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    fun listFileWithTask(task: Task){
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Loading
                val result = taskRepository.listFileWithTask(task)
                result.onSuccess {
                    _taskState.value = TaskUIState.SuccessGetListFile(it)
                }.onFailure { e ->
                    _taskState.value = TaskUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    fun mapTaskWithUser(user: User){
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Loading
                val result = taskRepository.mapTaskWithUser(user)
                result.onSuccess {
                    _taskState.value = TaskUIState.SuccessMapTask(it)
                }.onFailure { e ->
                    _taskState.value = TaskUIState.Error(e.message.toString())
                }
            } catch (e: Exception) {
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    fun loadAllTasks(userId: String, groupId: String?) {
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Loading

                val result = if (groupId == null) {
                    taskRepository.listTaskAllOfUser(userId)
                } else {
                    taskRepository.listTaskInGroupOfUser(groupId, userId)
                }

                result.onSuccess { list ->
                    // Lưu toàn bộ task
                    _tasks.value = list

                    // Đếm theo ngày của user đó
                    _countByDay.value = list
                        .mapNotNull { task -> task.dateOnly[userId] }
                        .groupingBy { it }
                        .eachCount()

                    _taskState.value = TaskUIState.Idle
                }.onFailure { e ->
                    _taskState.value = TaskUIState.Error(e.message ?: "Error")
                }

            } catch (e: Exception) {
                _taskState.value = TaskUIState.Error(e.message ?: "Error")
            }
        }
    }

    //time: 0 là all, 1 là week, 2 là month
    fun getProgressGroup(time: Int, groupId: String){
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Loading
                val result = taskRepository.getProgressGroup(time, groupId)
                result.onSuccess {
                    _taskState.value = TaskUIState.SuccessGetProgressGroup(it)
                }.onFailure { e ->
                    _taskState.value = TaskUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    fun getProgressMember(time: Int, groupId: String){
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Loading
                val result = taskRepository.getProgressMember(time, groupId)
                result.onSuccess {
                    _taskState.value = TaskUIState.SuccessGetProgressMember(it)
                }.onFailure { e ->
                    _taskState.value = TaskUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    fun deleteTask(task: Task){
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Loading
                val result = taskRepository.deleteTask(task)
                result.onSuccess {
                    _taskState.value = TaskUIState.SuccessDelete(it)
                }.onFailure { e ->
                    _taskState.value = TaskUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    fun updateTask(id: String, input: TaskInput) {
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Idle
                validateCreate(
                    input.title, input.description, input.typeDeadline, input.state,
                    input.quantity, input.type, input.assignedTo, input.group.type
                )
                if (_taskState.value != TaskUIState.Idle) return@launch

                Log.d("updateTask", "updateTask: $id")
                _taskState.value = TaskUIState.Loading

                val result = taskRepository.updateTask(id, input)
                result.onSuccess { _taskState.value = TaskUIState.SuccessUpdate(it) }
                    .onFailure { e -> _taskState.value = TaskUIState.Error(e.message.toString()) }
            } catch (e: Exception) {
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    fun updateStatus(task: Task, status: Int, userId: String){
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Loading
                val result = taskRepository.updateStatus(task, status, userId)
                result.onSuccess {
                    _taskState.value = TaskUIState.SuccessUpdateStatus(it)
                }.onFailure { e ->
                    _taskState.value = TaskUIState.Error(e.message.toString())
                }
            }catch (e: Exception){
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    fun createTask(input: TaskInput) {
        viewModelScope.launch {
            try {
                _taskState.value = TaskUIState.Idle
                validateCreate(
                    input.title, input.description, input.typeDeadline, input.state,
                    input.quantity, input.type, input.assignedTo, input.group.type
                )
                if (_taskState.value != TaskUIState.Idle) return@launch

                _taskState.value = TaskUIState.Loading
                val result = taskRepository.createTask(input)
                result.onSuccess { _taskState.value = TaskUIState.SuccessCreate(it) }
                    .onFailure { e -> _taskState.value = TaskUIState.Error(e.message.toString()) }
            } catch (e: Exception) {
                _taskState.value = TaskUIState.Error(e.message.toString())
            }
        }
    }

    private fun validateCreate(title: String, description: String, typeDeadline: Int, state: Int, quantity: Int, type: Int, assignedTo: String, groupType: Int) {
        if (!ValidateUtil.emptyCheckTaskName(title)) {
            _taskState.value = TaskUIState.EmptyNameTask
        } else if (!ValidateUtil.emptyCheckDescriptionTask(description)) {
            _taskState.value = TaskUIState.EmptyDescriptionTask
        } else if (!ValidateUtil.emptyCheckQuantity(quantity) && typeDeadline == 0) {
            _taskState.value = TaskUIState.EmptyQuantity
        } else if (!ValidateUtil.emptyCheckType(type) && typeDeadline == 0) {
            _taskState.value = TaskUIState.EmptyType
        } else if (!ValidateUtil.conditionCheckDate(state) && (typeDeadline == 1 || typeDeadline == 2)) {
            _taskState.value = TaskUIState.ConditionDate
        } else if (!ValidateUtil.emptyAssignedTo(assignedTo) && groupType == 2) {
            _taskState.value = TaskUIState.EmptyAssignedTo
        }
    }
}