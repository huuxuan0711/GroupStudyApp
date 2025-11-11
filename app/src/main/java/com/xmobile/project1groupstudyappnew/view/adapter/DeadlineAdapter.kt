package com.xmobile.project1groupstudyappnew.view.adapter

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.task.Task

class DeadlineAdapter(
    private val inGroup: Boolean,
    private val userId: String,
    private val context: Context,
    private val onItemClick: (Task) -> Unit
) : ListAdapter<Task, DeadlineAdapter.DeadlineViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeadlineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_deadline_list, parent, false)
        return DeadlineViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeadlineViewHolder, position: Int) {
        val task = getItem(position)

        if (!inGroup){
            holder.txt1.text = context.getString(R.string.group2)
            holder.deadlineName.text = task.groupName
        } else {
            holder.txt1.text = context.getString(R.string.creator)
            holder.deadlineName.text = task.nameCreatedBy
        }

        holder.deadlineTitle.text = task.title
        holder.deadlineDate.text = task.hourOnly.ifEmpty { context.getString(R.string.all_day) }

        val color = when (task.status[userId]) {
            0 -> ContextCompat.getColor(context, R.color.blue)
            1, 2 -> ContextCompat.getColor(context, R.color.yellow)
            3 -> ContextCompat.getColor(context, R.color.green)
            4 -> ContextCompat.getColor(context, R.color.gray)
            else -> ContextCompat.getColor(context, R.color.black)
        }

        holder.deadlineState.setColorFilter(color, PorterDuff.Mode.SRC_IN)

        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    inner class DeadlineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deadlineTitle: TextView = itemView.findViewById(R.id.deadline_title)
        val deadlineDate: TextView = itemView.findViewById(R.id.task_deadline)
        val deadlineName: TextView = itemView.findViewById(R.id.name)
        val deadlineState: ImageView = itemView.findViewById(R.id.image_deadline_state)
        val txt1: TextView = itemView.findViewById(R.id.txt1)
    }
}
