package com.xmobile.project1groupstudyappnew.repository

import android.content.Context
import com.xmobile.project1groupstudyappnew.model.obj.member.Member
import com.xmobile.project1groupstudyappnew.model.obj.user.User

interface UserRepository {
    suspend fun getUserInfo(uid: String): Result<User>
    suspend fun getMemberInfo(groupId: String, userId: String): Result<Member>
    suspend fun listMember(groupId: String): Result<List<Member>>
    suspend fun logout(uid: String, context: Context): Result<Boolean>
    suspend fun resetPassword(oldPassword: String, newPassword: String): Result<Boolean>
    suspend fun modifyInfo(userName: String, userDescription: String, avatar: String, uid: String): Result<User>
}