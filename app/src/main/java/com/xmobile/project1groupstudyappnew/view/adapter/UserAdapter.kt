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
import com.xmobile.project1groupstudyappnew.model.obj.user.User
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader

class UserAdapter(
    private val onItemClicked: (User) -> Unit,
    private val onImageProfileClicked: (User) -> Unit
) : ListAdapter<User, UserAdapter.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_member_list, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = getItem(position)
        holder.tvName.text = user.name
        AvatarLoader.load(holder.itemView.context, holder.avatar, user.avatar, user.name)

        holder.itemView.setOnClickListener { onItemClicked(user) }
        holder.imageProfile.setOnClickListener { onImageProfileClicked(user) }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.member_name)
        val avatar: ImageView = itemView.findViewById(R.id.member_avatar)
        val imageProfile: ImageView = itemView.findViewById(R.id.imgProfile)
    }
}
