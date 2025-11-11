package com.xmobile.project1groupstudyappnew.model.state

sealed class PasswordUIState {
    data object Idle : PasswordUIState()
    data object Loading : PasswordUIState()
    data object  EmptyEmail: PasswordUIState()
    data object  FormatEmail: PasswordUIState()
    data class Success(val email: String) : PasswordUIState()
    data class Error(val message: String) : PasswordUIState()
}