package com.xmobile.project1groupstudyappnew.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.Message
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val context: Context,
    private val userId: String,
    private val onItemClick: (Message) -> Unit,
    private val onLongClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == userId) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            MyViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_me, parent, false)
            )
        } else {
            OtherViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_other, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val isFile = message.file?.id?.isNotEmpty() == true

        if (holder is MyViewHolder) {
            bindMessage(holder, message, isFile)
        } else if (holder is OtherViewHolder) {
            bindMessage(holder, message, isFile)
            AvatarLoader.load(context, holder.avatar, message.avatar, message.memberName)
        }

        holder.itemView.setOnClickListener {
            if (isFile) onItemClick(message)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(message)
            true
        }
    }


    private fun bindMessage(holder: RecyclerView.ViewHolder, message: Message, isFile: Boolean) {
        when (holder) {
            is MyViewHolder -> {
                holder.chatContent.visibility = if (isFile) View.GONE else View.VISIBLE
                holder.cardViewFile.visibility = if (isFile) View.VISIBLE else View.GONE
                if (isFile) bindFile(holder, message)
                else holder.chatContent.text = message.text ?: ""
            }
            is OtherViewHolder -> {
                holder.chatContent.visibility = if (isFile) View.GONE else View.VISIBLE
                holder.cardViewFile.visibility = if (isFile) View.VISIBLE else View.GONE
                if (isFile) bindFile(holder, message)
                else holder.chatContent.text = message.text ?: ""
            }
        }
    }

    private fun bindFile(holder: RecyclerView.ViewHolder, message: Message) {
        message.file?.let { file ->
            when (holder) {
                is MyViewHolder -> {
                    holder.fileName.text = file.name ?: ""
                    holder.fileCapacity.text = getCapacity(file.capacity)
                    holder.imageFileCategory.setImageResource(getDrawable(file.type))
                }
                is OtherViewHolder -> {
                    holder.fileName.text = file.name ?: ""
                    holder.fileCapacity.text = getCapacity(file.capacity)
                    holder.imageFileCategory.setImageResource(getDrawable(file.type))
                }
            }
        }
    }

    private fun formatTime(ts: Long?): String {
        return ts?.let {
            val date = Date(it)
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(date)
        } ?: ""
    }

    private fun getCapacity(capacity: Long): String {
        val kb = capacity / 1024.0
        val mb = kb / 1024.0
        return when {
            capacity < 1024 -> "$capacity B"
            capacity < 1024 * 1024 -> "%.2f KB".format(kb)
            else -> "%.2f MB".format(mb)
        }
    }

    private fun getDrawable(type: Int): Int {
        return when (type) {
            0 -> R.drawable.ic_pdf
            1 -> R.drawable.ic_image
            2 -> R.drawable.ic_video
            else -> R.drawable.ic_file
        }
    }

    inner class OtherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatContent: TextView = itemView.findViewById(R.id.txtChatContent)
        val cardViewFile: CardView = itemView.findViewById(R.id.cardViewFile)
        val imageFileCategory: ImageView = itemView.findViewById(R.id.image_file_category)
        val avatar: ImageView = itemView.findViewById(R.id.avtar_image)
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val fileCapacity: TextView = itemView.findViewById(R.id.file_capacity)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatContent: TextView = itemView.findViewById(R.id.txtChatContent)
        val cardViewFile: CardView = itemView.findViewById(R.id.cardViewFile)
        val imageFileCategory: ImageView = itemView.findViewById(R.id.image_file_category)
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val fileCapacity: TextView = itemView.findViewById(R.id.file_capacity)
    }
}
