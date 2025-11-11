package com.xmobile.project1groupstudyappnew.utils

import com.xmobile.project1groupstudyappnew.model.obj.group.GroupProgress
import com.xmobile.project1groupstudyappnew.model.obj.task.Task

object CalculateProgress {
    fun calculateProgress(tasks: List<Task>, time: Int): GroupProgress {
        var doneCount = 0
        var inProgressCount = 0
        var todoCount = 0

        val now = System.currentTimeMillis()
        val millisRange = when (time) {
            1 -> 7 * 24 * 60 * 60 * 1000L
            2 -> 30 * 24 * 60 * 60 * 1000L
            else -> Long.MAX_VALUE
        }

        for (task in tasks) {
            if (now - task.createdAt > millisRange) continue
            for (status in task.status.values) {
                when (status) {
                    3 -> doneCount++
                    1 -> inProgressCount++
                    0 -> todoCount++
                }
            }
        }

        return GroupProgress(0, "", doneCount, inProgressCount, todoCount)
    }
}