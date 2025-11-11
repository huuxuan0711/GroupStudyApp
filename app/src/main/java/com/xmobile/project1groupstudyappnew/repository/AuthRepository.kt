package com.xmobile.project1groupstudyappnew.repository

import android.app.Activity
import android.content.Context
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser>
    suspend fun registerWithEmail(email: String, password: String, confirmPassword: String, userName: String): Result<FirebaseUser>
    suspend fun loginWithGoogle(context: Context): Result<FirebaseUser>
    suspend fun loginWithMicrosoft(activity: Activity): Result<FirebaseUser>
    suspend fun saveToDatabase(user: FirebaseUser): Result<Boolean>
    suspend fun logout(): Result<Boolean>
}