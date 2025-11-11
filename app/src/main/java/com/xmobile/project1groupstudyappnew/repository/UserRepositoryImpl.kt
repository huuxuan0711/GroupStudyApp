package com.xmobile.project1groupstudyappnew.repository

import android.content.Context
import com.facebook.login.LoginManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.xmobile.project1groupstudyappnew.model.obj.member.Member
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager

class UserRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
): UserRepository {

    override suspend fun getUserInfo(uid: String): Result<User> {
        return try {
            val db = firebaseDatabase.reference
            val userRef = db.child("users").child(uid)

            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("User not found"))
            }

            val user = snapshot.getValue(User::class.java)
                ?: return Result.failure(Exception("Invalid user data"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMemberInfo(
        groupId: String,
        userId: String
    ): Result<Member> {
        return try {
            val db = firebaseDatabase.reference
            val memberRef = db.child("members").child(groupId).child(userId)
            val snapshot = memberRef.get().await()
            val member = snapshot.getValue(Member::class.java)
                ?: return Result.failure(Exception("Invalid member data"))
            Result.success(member)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listMember(groupId: String): Result<List<Member>> {
        return try {
            val db = firebaseDatabase.reference
            val memberRef = db.child("members").child(groupId)
            val snapshot = memberRef.get().await()
            val members = snapshot.children.mapNotNull { child ->
                child.getValue(Member::class.java)
            }
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(uid: String, context: Context): Result<Boolean> {
        return try {
            val currentUser = firebaseAuth.currentUser

            // Nếu không có user nào đang đăng nhập
            if (currentUser == null) {
                return Result.failure(Exception("Không có người dùng nào đang đăng nhập"))
            }

            // Lấy danh sách provider mà user đăng nhập (email/google/facebook/microsoft)
            val providerData = currentUser.providerData

            for (profile in providerData) {
                when (profile.providerId) {
                    GoogleAuthProvider.PROVIDER_ID -> {
                        // Đăng xuất khỏi Credential Manager
                        try {
                            val credentialManager = CredentialManager.create(context)
                            credentialManager.clearCredentialState(
                                ClearCredentialStateRequest()
                            )
                        } catch (_: Exception) { }
                    }

                    FacebookAuthProvider.PROVIDER_ID -> {
                        try {
                            LoginManager.getInstance().logOut()
                        } catch (_: Exception) { }
                    }

                    "microsoft.com" -> {

                    }

                    EmailAuthProvider.PROVIDER_ID -> {
                    }
                }
            }

            // Logout khỏi Firebase
            firebaseAuth.signOut()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(
        oldPassword: String,
        newPassword: String
    ): Result<Boolean> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Không có người dùng nào đang đăng nhập"))
            }
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, oldPassword)
            currentUser.reauthenticate(credential).await()
            currentUser.updatePassword(newPassword).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun modifyInfo(
        userName: String,
        userDescription: String,
        avatar: String,
        uid: String
    ): Result<User> {
        return try {
            val db = firebaseDatabase.reference
            val userRef = db.child("users").child(uid)

            // Lấy dữ liệu user hiện tại
            val snapshot = userRef.get().await()
            val user = snapshot.getValue(User::class.java)
                ?: return Result.failure(Exception("Invalid user data"))

            // Tạo user mới đã cập nhật
            val newUser = user.copy(
                name = userName,
                description = userDescription,
                avatar = avatar
            )

            // Update vào node users
            userRef.setValue(newUser).await()

            // Update vào node members
            val memberQuery = db.child("members")
                .orderByChild("userId")
                .equalTo(uid)
                .get()
                .await()

            memberQuery.children.forEach { memberSnapshot ->
                val member = memberSnapshot.getValue(Member::class.java)
                if (member != null) {
                    val updatedMember = member.copy(
                        memberName = userName,
                        avatar = avatar
                    )
                    memberSnapshot.ref.setValue(updatedMember).await()
                }
            }

            Result.success(newUser)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}