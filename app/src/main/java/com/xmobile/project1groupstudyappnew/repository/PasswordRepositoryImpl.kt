package com.xmobile.project1groupstudyappnew.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PasswordRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
): PasswordRepository {
    override suspend fun sendEmail(email: String): Result<String> {
        try {
            firebaseAuth.setLanguageCode("vi")
            firebaseAuth.sendPasswordResetEmail(email).await()
            return Result.success(email)
        }catch (e: Exception) {
            return Result.failure(e)
        }
    }
}