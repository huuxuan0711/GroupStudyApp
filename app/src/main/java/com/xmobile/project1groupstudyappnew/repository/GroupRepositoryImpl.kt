package com.xmobile.project1groupstudyappnew.repository

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.group.GroupInvite
import com.xmobile.project1groupstudyappnew.model.obj.member.Member
import com.xmobile.project1groupstudyappnew.model.obj.Message
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import com.xmobile.project1groupstudyappnew.utils.CreateInviteCode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GroupRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val mediaManager: MediaManager
): GroupRepository{

    override suspend fun listGroup(uid: String): Result<List<Group>> {
        return try {
            val db = firebaseDatabase.reference
            val userRef = db.child("users").child(uid)
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("User not found"))
            }
            val user = snapshot.getValue(User::class.java)
                ?: return Result.failure(Exception("Invalid user data"))
            val groups = user.groups
            val groupList = mutableListOf<Group>()
            for (groupId in groups.keys) {
                val groupSnapshot = db.child("groups").child(groupId).get().await()
                if (groupSnapshot.exists()) {
                    val group = groupSnapshot.getValue(Group::class.java)
                    groupList.add(group!!)
                }
            }
            Result.success(groupList)
        } catch (e: Exception) {
            Log.e("GroupRepositoryImpl", "Error fetching groups: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun addGroup(name: String, description: String, type: Int, uid: String): Result<Group> {
        return try {
            val db = firebaseDatabase.reference

            val groupId = db.child("groups").push().key ?: return Result.failure(Exception("Failed to generate groupId"))

            val inviteCode = CreateInviteCode.createInviteCode(groupId)
            Log.d("GroupRepositoryImpl", "Generated invite code: $inviteCode")

            val newGroup = Group(
                groupId = groupId,
                name = name,
                description = description,
                type = type,
                ownerId = uid,
                size = 1, createdAt = System.currentTimeMillis(), inviteCode = inviteCode
            )

            // Lưu group
            db.child("groups")
                .child(newGroup.groupId)
                .setValue(newGroup)
                .await()

            // Lấy thông tin user từ uid
            val userRef = db.child("users").child(uid)
            val userSnapshot = userRef.get().await()
            if (!userSnapshot.exists()) {
                return Result.failure(Exception("User not found"))
            }
            val user = userSnapshot.getValue(User::class.java)
                ?: return Result.failure(Exception("Invalid user data"))

            // Thêm người tạo vào members với role = 1 (chủ phòng)
            val member = Member(
                groupId = newGroup.groupId,
                userId = uid,
                role = 1,
                joinedAt = System.currentTimeMillis(),
                memberName = user.name,
                avatar = user.avatar
            )
            db.child("members")
                .child(newGroup.groupId)
                .child(uid)
                .setValue(member)
                .await()

            // Cập nhật groups trong user
            val updatedGroups = user.groups.toMutableMap()
            updatedGroups[newGroup.groupId] = true
            userRef.child("groups").setValue(updatedGroups).await()

            Result.success(newGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun checkGroupExist(uid: String, inviteCode: String): Result<Group> {
        return try {
            val db = firebaseDatabase.reference
            val userRef = db.child("users").child(uid)

            // Lấy dữ liệu user
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("User not found in database"))
            }

            val currentUser = snapshot.getValue(User::class.java)
                ?: return Result.failure(Exception("Invalid user data"))

            // Tìm groupId dựa trên inviteCode
            val groupsRef = db.child("groups")
            val querySnapshot = groupsRef.orderByChild("inviteCode")
                .equalTo(inviteCode)
                .get()
                .await()

            if (!querySnapshot.exists()) {
                return Result.failure(Exception("Invalid invite code"))
            }

            // lấy group đầu tiên khớp inviteCode
            val groupSnapshot = querySnapshot.children.first()
            val group = groupSnapshot.getValue(Group::class.java) ?: return Result.failure(Exception("Invalid group data"))

            // Nếu đã tham gia group
            if (currentUser.groups.containsKey(group.groupId)) {
                return Result.failure(Exception("Group already exists"))
            }

            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun getGroupFromId(groupId: String): Result<Group> {
        return try {
            val db = firebaseDatabase.reference
            val groupRef = db.child("groups").child(groupId)
            val snapshot = groupRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("Group not found"))
            }
            val group = snapshot.getValue(Group::class
                .java) ?: return Result.failure(Exception("Invalid group data"))
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadAvatarToCloudinary(imageUri: Uri, id: String): Result<String> {
        return try {
            val url = suspendCoroutine { continuation ->
                mediaManager.upload(imageUri)
                    .option("public_id", "avatars/$id") //groupId hoặc userId
                    .unsigned("unsigned_preset")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val secureUrl = resultData["secure_url"] as? String
                            if (secureUrl != null) {
                                continuation.resume(secureUrl)
                            } else {
                                continuation.resumeWithException(Exception("Không lấy được secure_url"))
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            continuation.resumeWithException(Exception(error.description))
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            continuation.resumeWithException(Exception(error.description))
                        }
                    })
                    .dispatch()
            }

            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun modifyInfo(
        name: String,
        description: String,
        avatar: String,
        groupId: String
    ): Result<Boolean> {
        return try {
            val groupRef = firebaseDatabase.getReference("groups").child(groupId)

            val updates = mapOf(
                "name" to name,
                "description" to description,
                "avatar" to avatar
            )

            groupRef.updateChildren(updates).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkRole(
        uid: String,
        groupId: String,
    ): Result<Int> {
        return try {
            val memberRef = firebaseDatabase.getReference("members")
                .child(groupId)
                .child(uid)

            val snapshot = memberRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Member not found"))
            }

            val member = snapshot.getValue(Member::class.java)
                ?: return Result.failure(Exception("Invalid member data"))

            Result.success(member.role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun authorizeMember(
        groupId: String,
        userId: String,
        role: Int
    ): Result<Boolean> {
        return try {
            val newRole = if (role == 2) 3 else 2
            val memberRef = firebaseDatabase.getReference("members")
                .child(groupId)
                .child(userId)
            val updates = mapOf("role" to newRole)
            memberRef.updateChildren(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMember(
        groupId: String,
        userId: String
    ): Result<Boolean> {
        return try {
            val db = firebaseDatabase.reference

            // Xóa member trong members
            db.child("members").child(groupId).child(userId).removeValue().await()

            // Xóa group trong user.groups
            db.child("users").child(userId).child("groups").child(groupId).removeValue().await()

            // Cập nhật lại các task
            val taskRef = db.child("tasks").child(groupId)
            val taskSnapshot = taskRef.get().await()
            for (task in taskSnapshot.children) {
                val taskData = task.getValue(Task::class.java) ?: continue
                val updatedDateOnly = taskData.dateOnly.toMutableMap()
                val updatedDeadlineNotified = taskData.deadlineNotified.toMutableMap()
                val updatedStatus = taskData.status.toMutableMap()

                updatedDateOnly.remove(userId)
                updatedDeadlineNotified.remove(userId)
                updatedStatus.remove(userId)

                val updates = mapOf(
                    "dateOnly" to updatedDateOnly,
                    "deadlineNotified" to updatedDeadlineNotified,
                    "status" to updatedStatus
                )
                task.ref.updateChildren(updates).await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun inviteMember(
        groupId: String,
        invitee: String,
        inviter: String?
    ): Result<Boolean> {
        return try {
            val db = firebaseDatabase.reference

            val groupSnapshot = db.child("groups").child(groupId).get().await()
            val group = groupSnapshot.getValue(Group::class.java)
                ?: return Result.failure(Exception("Group not found"))

            // Tạo id duy nhất cho lời mời
            val inviteId = db.child("group_invites").push().key
                ?: return Result.failure(Exception("Failed to generate inviteId"))

            val invite = GroupInvite(
                id = inviteId,
                inviteCode = group.inviteCode,
                inviterId = inviter ?: "",
                inviteeId = invitee,
                status = "pending",
                timestamp = System.currentTimeMillis()
            )

            db.child("group_invites")
                .child(groupId)
                .child(inviteId)
                .setValue(invite)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLastSeenMessageId(groupId: String, messageId: String): Result<Boolean> {
        return try {
            val db = firebaseDatabase.reference
            db.child("groups").child(groupId).child("lastSeenMessageId").setValue(messageId).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinGroup(uid: String, group: Group): Result<Group> {
        return try {
            val db = firebaseDatabase.reference

            // Lấy thông tin user
            val userRef = db.child("users").child(uid)
            val userSnapshot = userRef.get().await()
            if (!userSnapshot.exists()) return Result.failure(Exception("User not found"))
            val currentUser = userSnapshot.getValue(User::class.java)
                ?: return Result.failure(Exception("Invalid user data"))

            // Kiểm tra group
            val groupRef = db.child("groups").child(group.groupId)
            val groupSnapshot = groupRef.get().await()
            val groupData = groupSnapshot.getValue(Group::class.java)
                ?: return Result.failure(Exception("Invalid group data"))

            // Cập nhật lời mời (nếu có)
            val invitesRef = db.child("group_invites").child(group.groupId)
            val invitesSnapshot = invitesRef.get().await()
            for (inviteSnap in invitesSnapshot.children) {
                val invite = inviteSnap.getValue(GroupInvite::class.java) ?: continue
                if (invite.inviteeId == uid && invite.status == "pending") {
                    invitesRef.child(invite.id).child("status").setValue("accepted").await()
                }
            }

            // Cập nhật groups trong user
            val updatedGroups = currentUser.groups.toMutableMap()
            updatedGroups[group.groupId] = true
            userRef.child("groups").setValue(updatedGroups).await()

            // Thêm user vào members
            val member = Member(
                groupId = group.groupId,
                userId = uid,
                role = 2, // role = member
                joinedAt = System.currentTimeMillis(),
                memberName = currentUser.name,
                avatar = currentUser.avatar
            )
            db.child("members").child(group.groupId).child(uid).setValue(member).await()

            // Cập nhật tất cả task của group cho user mới
            val taskRef = db.child("tasks").child(group.groupId)
            val taskSnapshot = taskRef.get().await()
            for (task in taskSnapshot.children) {
                val taskData = task.getValue(Task::class.java) ?: continue
                val updatedDateOnly = taskData.dateOnly.toMutableMap()
                val updatedDeadlineNotified = taskData.deadlineNotified.toMutableMap()
                val updatedStatus = taskData.status.toMutableMap()

                updatedDateOnly[uid] = ""
                updatedDeadlineNotified[uid] = false
                updatedStatus[uid] = 0

                val updates = mapOf(
                    "dateOnly" to updatedDateOnly,
                    "deadlineNotified" to updatedDeadlineNotified,
                    "status" to updatedStatus
                )
                task.ref.updateChildren(updates).await()
            }

            // Cập nhật size group
            val newSize = groupData.size + 1
            groupRef.child("size").setValue(newSize).await()

            Result.success(groupData.copy(size = newSize))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(uid: String, groupId: String): Result<Boolean> {
        return try {
            val db = firebaseDatabase.reference
            val groupRef = db.child("groups").child(groupId)
            val groupSnapshot = groupRef.get().await()
            if (!groupSnapshot.exists()) return Result.failure(Exception("Group not found"))
            val groupData = groupSnapshot.getValue(Group::class.java) ?: return Result.failure(
                Exception("Invalid group data")
            )

            val userRef = db.child("users").child(uid)
            val userSnapshot = userRef.get().await()
            val currentUser = userSnapshot.getValue(User::class.java) ?: return Result.failure(
                Exception("Invalid user data")
            )

            if (!currentUser.groups.containsKey(groupId)) return Result.failure(Exception("User not in group"))

            // Nếu chủ phòng hoặc nhóm còn 1 người → xóa group
            if (groupData.size <= 1 || groupData.ownerId == uid) {
                // 1. Lấy tất cả members của group trước khi xóa
                val membersSnapshot = db.child("members").child(groupId).get().await()
                val memberIds = membersSnapshot.children.mapNotNull { it.getValue(Member::class.java)?.userId }

                // 2. Xóa group, members, tasks
                groupRef.removeValue().await()
                db.child("members").child(groupId).removeValue().await()
                db.child("tasks").child(groupId).removeValue().await()

                // 3. Xóa group khỏi tất cả user
                for (memberId in memberIds) {
                    db.child("users").child(memberId).child("groups").child(groupId).removeValue().await()
                }

                return Result.success(true)
            }

            // Bình thường: rời nhóm
            val updatedGroups = currentUser.groups.toMutableMap()
            updatedGroups.remove(groupId)
            userRef.child("groups").setValue(updatedGroups).await()

            db.child("members").child(groupId).child(uid).removeValue().await()

            // Cập nhật lại task (xóa key user khỏi 3 map)
            val taskRef = db.child("tasks").child(groupId)
            val taskSnapshot = taskRef.get().await()
            for (task in taskSnapshot.children) {
                val taskData = task.getValue(Task::class.java) ?: continue
                val updatedDateOnly = taskData.dateOnly.toMutableMap()
                val updatedDeadlineNotified = taskData.deadlineNotified.toMutableMap()
                val updatedStatus = taskData.status.toMutableMap()

                updatedDateOnly.remove(uid)
                updatedDeadlineNotified.remove(uid)
                updatedStatus.remove(uid)

                val updates = mapOf(
                    "dateOnly" to updatedDateOnly,
                    "deadlineNotified" to updatedDeadlineNotified,
                    "status" to updatedStatus
                )
                task.ref.updateChildren(updates).await()
            }

            val newSize = groupData.size - 1
            groupRef.child("size").setValue(newSize).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun listenGroupUpdates(groupId: String): Flow<String> = callbackFlow {
        val db = firebaseDatabase.reference
        val listeners = mutableListOf<ChildEventListener>()

        fun <T> createListener(
            path: String,
            parse: (DataSnapshot) -> T?,
            formatMessage: (T) -> String,
            onChildChanged: ((T) -> String)? = null,
            onChildRemoved: ((T) -> String)? = null
        ): ChildEventListener {
            val query = db.child(path).orderByChild("groupId").equalTo(groupId)
            val listener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val value = parse(snapshot) ?: return
                    trySend(formatMessage(value))
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    if (onChildChanged != null) {
                        val value = parse(snapshot) ?: return
                        trySend(onChildChanged(value))
                    }
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    if (onChildRemoved != null) {
                        val value = parse(snapshot) ?: return
                        trySend(onChildRemoved(value))
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            }
            query.addChildEventListener(listener)
            listeners.add(listener)
            return listener
        }


        // Tin nhắn mới
        createListener("messages",
            parse = { it.getValue(Message::class.java) },
            formatMessage = { msg ->
                if (msg.file != null)
                    "${msg.memberName} vừa gửi ${fileTypeToText(msg.file.type)} '${msg.file.name}'"
                else
                    "${msg.memberName}: ${msg.text.take(30)}"
            }
        )

        // Task thay đổi
        createListener("tasks",
            parse = { it.getValue(Task::class.java) },
            formatMessage = { task -> "${task.nameCreatedBy} đã tạo task '${task.title}'" },
            onChildChanged = { task -> "Task '${task.title}' vừa được cập nhật trạng thái" }
        )

        // File thay đổi
        createListener("files",
            parse = { it.getValue(File::class.java) },
            formatMessage = { file -> "${file.uploadedByName} vừa tải lên ${fileTypeToText(file.type)} '${file.name}'" }
        )

        // Thành viên thay đổi
        createListener("members",
            parse = { it.getValue(Member::class.java) },
            formatMessage = { member -> "${member.memberName} đã tham gia nhóm" },
            onChildRemoved = { member -> "${member.memberName} đã rời nhóm" }
        )

        awaitClose {
            listeners.forEach { listener ->
                db.removeEventListener(listener)
            }
        }
    }

    // Chuyển type file sang text
    private fun fileTypeToText(type: Int): String = when(type) {
        0 -> "file PDF"
        1 -> "hình ảnh"
        2 -> "video"
        else -> "tài liệu"
    }
}