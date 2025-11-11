package com.xmobile.project1groupstudyappnew.model.state

import com.xmobile.project1groupstudyappnew.model.obj.member.Member
import com.xmobile.project1groupstudyappnew.model.obj.user.User

sealed class UserUIState {
    data object Idle : UserUIState()
    data object Loading : UserUIState()
    data object EmptyName: UserUIState()
    data object ConditionName: UserUIState()
    data object ConditionDescription: UserUIState()
    data object SuccessLogout : UserUIState()
    data class SuccessGetMemberInfo(val member: Member) : UserUIState()
    data class SuccessGetInfo(val user: User) : UserUIState()
    data class SuccessListMember(val members: List<Member>) : UserUIState()
    data class Error(val message: String) : UserUIState()

}