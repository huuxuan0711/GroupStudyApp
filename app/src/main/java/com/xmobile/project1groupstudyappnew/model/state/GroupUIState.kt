package com.xmobile.project1groupstudyappnew.model.state

import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.member.Member

sealed class GroupUIState {
    data object Idle : GroupUIState()
    data object Loading : GroupUIState()
    data object EmptyName: GroupUIState()
    data object ConditionName: GroupUIState()
    data object ConditionDescription: GroupUIState()
    data class Role(val role: Int) : GroupUIState()
    data class SuccessUpload(val url: String) : GroupUIState()
    data class SuccessModify(val name: String, val description: String) : GroupUIState()
    data object SuccessAuthorize : GroupUIState()
    data class SuccessAddMember(val member: Member) : GroupUIState()
    data class SuccessDeleteMember(val userId: String) : GroupUIState()
    data object SuccessLeave : GroupUIState()
    data class SuccessGetGroup(val group: Group) : GroupUIState()
    data class SuccessInviteMember(val userId: String) : GroupUIState()
    data class Error(val message: String) : GroupUIState()
}