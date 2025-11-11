package com.xmobile.project1groupstudyappnew.repository

import com.google.firebase.database.FirebaseDatabase
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.model.obj.group.GroupProgress
import com.xmobile.project1groupstudyappnew.model.obj.member.Member
import com.xmobile.project1groupstudyappnew.model.obj.member.MemberProgress
import com.xmobile.project1groupstudyappnew.model.obj.task.Task
import com.xmobile.project1groupstudyappnew.model.obj.task.TaskInput
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
): TaskRepository {

    override suspend fun listTask(groupId: String): Result<List<Task>> {
        return try {
            val db = firebaseDatabase.reference
            val snapshot = db.child("tasks")
                .orderByChild("createdAt")
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.success(emptyList())
            }

            val tasks = snapshot.children
                .mapNotNull { it.getValue(Task::class.java) }
                .filter { it.groupId == groupId }

            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listTaskAllOfUser(userId: String): Result<List<Task>> {
        return try {
            val db = firebaseDatabase.reference
            val snapshot = db.child("tasks")
                .orderByChild("createdAt")
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.success(emptyList())
            }
            val tasks = snapshot.children
                .mapNotNull { it.getValue(Task::class.java) }
                .filter { it.status.containsKey(userId) }
            Result.success(tasks)
        }catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listTaskInGroupOfUser(
        groupId: String,
        userId: String
    ): Result<List<Task>> {
        return try {
            val db = firebaseDatabase.reference
            val snapshot = db.child("tasks")
                .orderByChild("createdAt")
                .get()
                .await()
            if (!snapshot.exists()) {
                return Result.success(emptyList())
            }
            val tasks = snapshot.children
                .mapNotNull { it.getValue(Task::class.java) }
                .filter { it.groupId == groupId && it.status.containsKey(userId) }
            Result.success(tasks)
        }catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun mapTaskWithUser(user: User): Result<Map<Int, List<Task>>> { //MAP: type group + list task
        return coroutineScope {
            try {
                val firebase = firebaseDatabase
                val groupRef = firebase.getReference("groups")
                val taskRef = firebase.getReference("tasks")

                val groupIds = user.groups.keys
                if (groupIds.isEmpty()) return@coroutineScope Result.success(emptyMap())

                // Lấy thông tin tất cả group của user song song
                val groupDeferreds = groupIds.map { groupId ->
                    async {
                        groupRef.child(groupId).get().await().getValue(Group::class.java)
                    }
                }

                val groups = groupDeferreds.awaitAll().filterNotNull()

                // Map groupId -> type
                val groupTypeMap = groups.associate { it.groupId to it.type }

                // Fetch tất cả task theo groupId song song
                val taskDeferreds = groupIds.map { groupId ->
                    async {
                        taskRef.orderByChild("groupId").equalTo(groupId).get().await().children.mapNotNull {
                            it.getValue(Task::class.java)
                        }.filter { task ->
                            task.status.containsKey(user.userId)
                        }
                    }
                }

                val allTasks = taskDeferreds.awaitAll().flatten()

                // Gom theo type group (1 hoặc 2)
                val groupedTasks = allTasks.groupBy { task ->
                    groupTypeMap[task.groupId] ?: 0
                }.filterKeys { it != 0 } // loại bỏ group không hợp lệ

                Result.success(groupedTasks)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    }

    override suspend fun listFileWithTask(task: Task): Result<List<File>> {
        return try {
            val fileIds = task.resource?.filterValues { it }?.keys
            val filesSnapshot = firebaseDatabase.getReference("files")
                .get()
                .await()

            val files = filesSnapshot.children
                .mapNotNull { it.getValue(File::class.java) }
                .filter { fileIds?.contains(it.id) == true  }

            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // time: 0 = all, 1 = week, 2 = month
    override suspend fun getProgressGroup(time: Int, groupId: String): Result<GroupProgress> {
        return try {
            val ref = firebaseDatabase.getReference("tasks")
            val snapshot = ref.orderByChild("groupId").equalTo(groupId).get().await()

            var doneCount = 0
            var inProgressCount = 0
            var todoCount = 0
            var groupName = ""

            val now = System.currentTimeMillis()
            val millisRange = when (time) {
                1 -> 7 * 24 * 60 * 60 * 1000L   // tuần
                2 -> 30 * 24 * 60 * 60 * 1000L  // tháng
                else -> Long.MAX_VALUE          // toàn bộ
            }

            for (taskSnap in snapshot.children) {
                val task = taskSnap.getValue(Task::class.java) ?: continue

                // chỉ lấy task trong khoảng thời gian
                if (now - task.createdAt > millisRange) continue

                groupName = task.groupName

                // đếm trạng thái trong task
                for (statusValue in task.status.values) {
                    when (statusValue) {
                        1 -> inProgressCount++
                        3 -> doneCount++
                        0 -> todoCount++
                    }
                }
            }

            val progress = GroupProgress(
                groupId = groupId.hashCode(),
                groupName = groupName,
                doneCount = doneCount,
                inProgressCount = inProgressCount,
                todoCount = todoCount
            )

            Result.success(progress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProgressMember(time: Int, groupId: String): Result<List<MemberProgress>> {
        return try {
            val tasksRef = firebaseDatabase.getReference("tasks")
            val membersRef = firebaseDatabase.getReference("members").child(groupId)

            val tasksSnapshot = tasksRef.orderByChild("groupId").equalTo(groupId).get().await()
            val membersSnapshot = membersRef.get().await()

            // Map userId -> Member
            val memberInfoMap = mutableMapOf<String, Member>()
            for (memSnap in membersSnapshot.children) {
                val member = memSnap.getValue(Member::class.java) ?: continue
                memberInfoMap[member.userId] = member
            }

            val memberProgressMap = mutableMapOf<String, MemberProgress>()

            val now = System.currentTimeMillis()
            val millisRange = when (time) {
                1 -> 7 * 24 * 60 * 60 * 1000L   // tuần
                2 -> 30 * 24 * 60 * 60 * 1000L  // tháng
                else -> Long.MAX_VALUE          // toàn bộ
            }

            for (taskSnap in tasksSnapshot.children) {
                val task = taskSnap.getValue(Task::class.java) ?: continue
                if (now - task.createdAt > millisRange) continue

                for ((uid, statusValue) in task.status) {
                    val existing = memberProgressMap[uid]
                    val done = (existing?.doneCount ?: 0) + if (statusValue == 2 || statusValue == 3) 1 else 0
                    val inProgress = (existing?.inProgressCount ?: 0) + if (statusValue == 1) 1 else 0
                    val todo = (existing?.notStartedCount ?: 0) + if (statusValue == 0) 1 else 0
                    val total = (existing?.totalCount ?: 0) + 1

                    val memberInfo = memberInfoMap[uid]

                    memberProgressMap[uid] = MemberProgress(
                        memberName = memberInfo?.memberName ?: "Unknown",
                        doneCount = done,
                        inProgressCount = inProgress,
                        notStartedCount = todo,
                        totalCount = total,
                        avatarUrl = memberInfo?.avatar
                    )
                }
            }

            Result.success(memberProgressMap.values.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun createTask(input: TaskInput): Result<Task> {
        return try {
            val dbRef = firebaseDatabase.reference
            val taskRef = dbRef.child("tasks").push()
            val taskId = taskRef.key ?: return Result.failure(Exception("Failed to generate taskId"))

            val membersSnapshot = dbRef.child("members").child(input.group.groupId).get().await()
            val members = membersSnapshot.children.mapNotNull { it.getValue(Member::class.java) }

            val createdByMember = members.find { it.userId == input.userId }
            val assignedToMember = members.find { it.userId == input.assignedTo }

            val nameCreatedBy = createdByMember?.memberName ?: "Unknown"
            val nameAssignedTo = assignedToMember?.memberName ?: "Unknown"

            val deadlineNotifiedMap: Map<String, Boolean> = when (input.typeDeadline) {
                1, 2 -> if (input.group.type == 2) mapOf(input.assignedTo to false)
                else members.associate { it.userId to false }
                else -> emptyMap()
            }

            val status: Map<String, Int> = if (input.group.type == 1) members.associate { it.userId to 0 }
            else mapOf(input.assignedTo to 0)

            val resourceMap = input.listFile?.associate { it.id to true }

            val dateOnlyMap: Map<String, String> = when (input.typeDeadline) {
                1, 2 -> if (input.group.type == 2) mapOf(input.assignedTo to input.dateOnly)
                else members.associate { it.userId to input.dateOnly }
                else -> emptyMap()
            }

            val newTask = Task(
                id = taskId,
                groupId = input.group.groupId,
                groupName = input.group.name,
                title = input.title,
                description = input.description,
                createdBy = input.userId,
                nameCreatedBy = nameCreatedBy,
                assignedTo = input.assignedTo,
                nameAssignedTo = nameAssignedTo,
                typeDeadline = input.typeDeadline,
                deadline = input.deadlineDisplay,
                quantity = input.quantity,
                type = input.type,
                dateOnly = dateOnlyMap,
                hourOnly = input.hourOnly,
                deadlineNotified = deadlineNotifiedMap,
                status = status,
                createdAt = System.currentTimeMillis(),
                resource = resourceMap
            )

            taskRef.setValue(newTask).await()
            Result.success(newTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTask(id: String, input: TaskInput): Result<Task> {
        return try {
            val dbRef = firebaseDatabase.reference
            val taskRef = dbRef.child("tasks").child(id)

            val snapshot = taskRef.get().await()
            val oldTask = snapshot.getValue(Task::class.java)
                ?: return Result.failure(Exception("Không tìm thấy task với id: $id"))

            val membersSnapshot = dbRef.child("members").child(input.group.groupId).get().await()
            val members = membersSnapshot.children.mapNotNull { it.getValue(Member::class.java) }

            val assignedToMember = members.find { it.userId == input.assignedTo }
            val nameAssignedTo = assignedToMember?.memberName ?: "Unknown"

            val safeResourceMap = input.listFile?.associate { it.id to true } ?: oldTask.resource

            val updatedDateOnly = when (input.typeDeadline) {
                1, 2 -> if (input.group.type == 2) mapOf(input.assignedTo to input.dateOnly)
                else members.associate { it.userId to input.dateOnly }
                0 -> if (input.group.type == 2) oldTask.dateOnly.toMutableMap().apply { remove(input.assignedTo) }
                else emptyMap()
                else -> oldTask.dateOnly
            }

            val updatedDeadlineNotified = when (input.typeDeadline) {
                1, 2 -> if (input.group.type == 2) mapOf(input.assignedTo to (oldTask.deadlineNotified[input.assignedTo] ?: false))
                else members.associate { member -> member.userId to (oldTask.deadlineNotified[member.userId] ?: false) }
                else -> oldTask.deadlineNotified
            }

            val updates = mutableMapOf(
                "title" to input.title,
                "description" to input.description,
                "assignedTo" to input.assignedTo,
                "nameAssignedTo" to nameAssignedTo,
                "typeDeadline" to input.typeDeadline,
                "deadline" to input.deadlineDisplay,
                "quantity" to input.quantity,
                "type" to input.type,
                "hourOnly" to input.hourOnly,
                "resource" to (safeResourceMap?.mapValues { it.value as Any } ?: emptyMap()),
                "dateOnly" to updatedDateOnly.mapValues { it.value as Any },
                "deadlineNotified" to updatedDeadlineNotified.mapValues { it.value as Any },
                "status" to oldTask.status.mapValues { it.value as Any }
            )

            taskRef.updateChildren(updates).await()

            val updatedTask = oldTask.copy(
                title = input.title,
                description = input.description,
                assignedTo = input.assignedTo,
                nameAssignedTo = nameAssignedTo,
                typeDeadline = input.typeDeadline,
                deadline = input.deadlineDisplay,
                quantity = input.quantity,
                type = input.type,
                dateOnly = updatedDateOnly,
                hourOnly = input.hourOnly,
                deadlineNotified = updatedDeadlineNotified,
                status = oldTask.status,
                resource = safeResourceMap
            )

            Result.success(updatedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(task: Task): Result<Task> {
        return try {
            val dbRef = firebaseDatabase.reference
            val taskRef = dbRef.child("tasks").child(task.id)

            // 🔹 Xóa task khỏi Firebase
            taskRef.removeValue().await()
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaskFromId(id: String): Result<Task> {
        return try {
            val dbRef = firebaseDatabase.getReference("tasks").child(id)
            val snapshot = dbRef.get().await()
            val task = snapshot.getValue(Task::class.java)
            if (task != null) {
                Result.success(task)
            } else {
                Result.failure(Exception("Không tìm thấy task với id: $id"))
            }
        }catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStatus(
        task: Task,
        status: Int,
        userId: String
    ): Result<Int> {
        return try {
            val dbRef = firebaseDatabase.getReference("tasks").child(task.id)

            // cập nhật map status
            val updatedStatus = task.status.toMutableMap()
            updatedStatus[userId] = status

            val updatedDateOnly = task.dateOnly.toMutableMap()

            // Nếu typeDeadline = 0 và status chuyển sang 1 (started)
            if (task.typeDeadline == 0 && status == 1) {
                val now = System.currentTimeMillis()

                // Xác định số mili giây cần cộng
                val millisToAdd = when (task.type) {
                    1 -> task.quantity * 60 * 60 * 1000L          // Giờ
                    2 -> task.quantity * 24 * 60 * 60 * 1000L     // Ngày
                    3 -> task.quantity * 7 * 24 * 60 * 60 * 1000L // Tuần
                    4 -> task.quantity * 30L * 24 * 60 * 60 * 1000L // Tháng (xấp xỉ)
                    else -> 0L
                }

                val deadlineTime = now + millisToAdd
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val formattedDate = sdf.format(deadlineTime)

                // Ghi lại ngày vào dateOnly map
                updatedDateOnly[userId] = formattedDate
            }

            // Cập nhật lên Firebase
            val updates = mapOf(
                "status" to updatedStatus,
                "dateOnly" to updatedDateOnly
            )
            dbRef.updateChildren(updates).await()

            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}