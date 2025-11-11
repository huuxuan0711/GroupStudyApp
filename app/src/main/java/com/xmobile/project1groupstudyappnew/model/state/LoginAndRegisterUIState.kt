package com.xmobile.project1groupstudyappnew.model.state

import com.google.firebase.auth.FirebaseUser

sealed class LoginAndRegisterUIState {
    data object Idle : LoginAndRegisterUIState()
    data object Loading : LoginAndRegisterUIState()
    data object  EmptyEmail: LoginAndRegisterUIState()
    data object  EmptyPassword: LoginAndRegisterUIState()
    data object  EmptyUserName: LoginAndRegisterUIState()
    data object  FormatEmail: LoginAndRegisterUIState()
    data object  ConditionPassword: LoginAndRegisterUIState()
    data object  MatchingPassword: LoginAndRegisterUIState()
    data object  ConditionUserName: LoginAndRegisterUIState()
    data class SuccessLogin(val user: FirebaseUser) : LoginAndRegisterUIState()
    data class SuccessRegister(val user: FirebaseUser) : LoginAndRegisterUIState()
    data object SuccessResetPassword: LoginAndRegisterUIState()
    data class Error(val message: String) : LoginAndRegisterUIState()
}