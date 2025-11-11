package com.xmobile.project1groupstudyappnew.model.state

import com.xmobile.project1groupstudyappnew.model.obj.group.Group

sealed class HomeUIState {
    data object Idle : HomeUIState()
    data object Loading : HomeUIState()
    data object EmptyName: HomeUIState()
    data object EmptyGroupId: HomeUIState()
    data object ConditionName: HomeUIState()
    data object ConditionDescription: HomeUIState()
    data class SuccessList(val groups: List<Group>) : HomeUIState()
    data class SuccessCreate(val group: Group): HomeUIState()
    data class SuccessJoin(val group: Group): HomeUIState()
    data class SuccessCheckExist(val group: Group): HomeUIState()
    data class Error(val message: String) : HomeUIState()

}