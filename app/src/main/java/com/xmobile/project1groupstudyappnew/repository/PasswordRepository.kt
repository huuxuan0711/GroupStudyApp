package com.xmobile.project1groupstudyappnew.repository

interface PasswordRepository {
    suspend fun sendEmail(email: String): Result<String>
}