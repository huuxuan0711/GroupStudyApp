package com.xmobile.project1groupstudyappnew.repository

import android.net.Uri
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun listGroup(uid: String): Result<List<Group>>
    suspend fun addGroup(name: String, description: String, type: Int, uid: String): Result<Group>
    suspend fun checkGroupExist(uid: String, inviteCode: String): Result<Group>
    suspend fun joinGroup(uid: String, group: Group): Result<Group>
    suspend fun leaveGroup(uid: String, groupId: String): Result<Boolean>
    suspend fun getGroupFromId(groupId: String): Result<Group>
    suspend fun uploadAvatarToCloudinary(imageUri: Uri, id: String): Result<String>
    suspend fun modifyInfo(name: String, description: String, avatar: String, groupId: String): Result<Boolean>
    suspend fun checkRole(uid: String, groupId: String): Result<Int>
    suspend fun authorizeMember(groupId: String, userId: String, role: Int): Result<Boolean>
    suspend fun deleteMember(groupId: String, userId: String): Result<Boolean>
    suspend fun inviteMember(groupId: String, invitee: String, inviter: String?): Result<Boolean>
    suspend fun updateLastSeenMessageId(groupId: String, messageId: String): Result<Boolean>
    fun listenGroupUpdates(groupId: String): Flow<String>
}