package com.xmobile.project1groupstudyappnew.view.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.task.Task

class TaskAdapter(
    private val userId: String,
    private val type: Int,
    private val context: Context,
    private val onItemClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallback()) {

    private var _status: Int = -1

    class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
    }

    fun listWithStatus(status: Int) {
        _status = status
        val filtered = if (status >= 0) {
            if (type == 1) {
                if (status == 0) currentList.filter { it.status.containsValue(0) }
                else currentList.filter { it.status[userId] == status }
            } else {
                currentList.filter { it.status[userId] == status }
            }
        } else {
            currentList
        }
        submitList(filtered.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task_list, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)

        val id: String = if (type == 1) userId
        else {
            if (_status == -1) task.assignedTo
            else userId
        }

        holder.taskState.apply {
            when (task.status[id]) {
                0 -> {
                    text = context.getString(R.string.to_do)
                    backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.blue))
                }
                1 -> {
                    text = context.getString(R.string.in_progress)
                    backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.yellow))
                }
                2 -> {
                    text = context.getString(R.string.review)
                    backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.yellow))
                }
                3 -> {
                    text = context.getString(R.string.done)
                    backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.green))
                }
                4 -> {
                    text = context.getString(R.string.canceled)
                    backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.gray))
                }
                else -> {
                    text = "Unknown"
                    backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.gray))
                }
            }
        }

        holder.taskTitle.text = task.title
        holder.taskDeadline.text = when (task.typeDeadline) {
            0 -> task.dateOnly[userId] ?: task.deadline
            1 -> task.deadline
            else -> task.deadline
        }

        if (type == 1) {
            holder.txt1.text = context.getString(R.string.creator)
            holder.memberName.text = task.nameCreatedBy
        } else {
            holder.txt1.text = context.getString(R.string.taker)
            holder.memberName.text = task.nameAssignedTo
        }

        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskState: TextView = itemView.findViewById(R.id.task_state)
        val taskTitle: TextView = itemView.findViewById(R.id.task_title)
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val txt1: TextView = itemView.findViewById(R.id.txt1)
        val taskDeadline: TextView = itemView.findViewById(R.id.task_deadline)
    }
}
