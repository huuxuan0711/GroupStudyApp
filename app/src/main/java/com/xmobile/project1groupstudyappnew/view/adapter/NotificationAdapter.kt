package com.xmobile.project1groupstudyappnew.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.Notification

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return NotificationViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_noti_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val noti = getItem(position)
        holder.notificationTitle.text = noti.title
        holder.notificationContent.text = noti.message
        holder.notificationDate.text = noti.timestamp.toString()

        holder.imgCategory.setImageResource(
            when (noti.type) {
                "task" -> R.drawable.ic_task
                "message" -> R.drawable.ic_chat
                "member" -> R.drawable.ic_member
                "file" -> R.drawable.ic_file
                "invite" -> R.drawable.ic_invite
                "deadline" -> R.drawable.ic_task
                else -> R.drawable.ic_noti
            }
        )

        holder.imgState.visibility = if (noti.read) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onItemClick(noti) }
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationTitle: TextView = itemView.findViewById(R.id.noti_title)
        val notificationContent: TextView = itemView.findViewById(R.id.noti_desciption)
        val notificationDate: TextView = itemView.findViewById(R.id.noti_time)
        val imgState: ImageView = itemView.findViewById(R.id.imgState)
        val imgCategory: ImageView = itemView.findViewById(R.id.image_category)
    }
}
