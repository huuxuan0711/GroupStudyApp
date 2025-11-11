package com.xmobile.project1groupstudyappnew.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import com.xmobile.project1groupstudyappnew.utils.CreateInviteCode
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
): AuthRepository {

    override suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null && user.isEmailVerified) {
                Result.success(user)
            } else {
                Result.failure(Exception("Không tồn tại tài khoản"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerWithEmail(email: String, password: String, confirmPassword: String, userName: String): Result<FirebaseUser> {
        return try {
            if (password != confirmPassword) {
                return Result.failure(Exception("Mật khẩu xác nhận không khớp"))
            }

            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val firebaseUser = authResult.user ?: return Result.failure(Exception("Không tạo được tài khoản"))

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            Result.success(firebaseUser)
        }catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            val idToken = if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                throw Exception("Không lấy được Google ID token")
            }

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Không tồn tại tài khoản"))
            }
        }catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithMicrosoft(activity: Activity): Result<FirebaseUser> {
        return try {
            val provider = OAuthProvider.newBuilder("microsoft.com")
            provider.addCustomParameter("prompt", "consent")

            val authResult = suspendCoroutine<AuthResult> { continuation ->
                firebaseAuth.startActivityForSignInWithProvider(activity, provider.build())
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }

//            val credential = authResult.credential as? OAuthCredential
//            val microsoftAccessToken = credential?.accessToken
//            val microsoftIdToken = credential?.idToken
//
//            Log.d("MicrosoftLogin", "AccessToken: $microsoftAccessToken")
//            Log.d("MicrosoftLogin", "IdToken: $microsoftIdToken")

            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveToDatabase(user: FirebaseUser): Result<Boolean> {
        return try {
            val userRef = firebaseDatabase.reference.child("users").child(user.uid)

            val snapshot = userRef.get().await()
            if (snapshot.exists()) {
                return Result.success(true)
            }

            val inviteCode = CreateInviteCode.createInviteCode(user.uid)
            val newUser = User(
                userId = user.uid,
                name = user.displayName ?: "",
                description = "",
                email = user.email ?: "",
                avatar = user.photoUrl?.toString() ?: "",
                groups = emptyMap(),
                inviteCode = inviteCode
            )

            userRef.setValue(newUser).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Boolean> {
        return try {
            firebaseAuth.signOut()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}