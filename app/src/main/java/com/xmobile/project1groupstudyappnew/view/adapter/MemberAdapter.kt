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
import com.xmobile.project1groupstudyappnew.model.obj.member.Member
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader

class MemberAdapter(
    private val onItemClicked: (Member) -> Unit,
    private val onImageProfileClicked: (String) -> Unit
) : ListAdapter<Member, MemberAdapter.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<Member>() {
        override fun areItemsTheSame(oldItem: Member, newItem: Member): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: Member, newItem: Member): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_member_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = getItem(position)

        if (member.role == 1 || member.role == 3) {
            holder.layoutAdmin.visibility = View.VISIBLE
            holder.tvName.visibility = View.GONE
            holder.ownerName.text = member.memberName
            holder.txtRole.text = holder.itemView.context.getString(
                if (member.role == 1) R.string.owner else R.string.admin
            )
        } else {
            holder.layoutAdmin.visibility = View.GONE
            holder.tvName.visibility = View.VISIBLE
            holder.tvName.text = member.memberName
        }

        AvatarLoader.load(holder.itemView.context, holder.avatar, member.avatar, member.memberName)

        holder.itemView.setOnClickListener { onItemClicked(member) }
        holder.imageProfile.setOnClickListener { onImageProfileClicked(member.userId) }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.member_name)
        val ownerName: TextView = itemView.findViewById(R.id.owner_name)
        val layoutAdmin: View = itemView.findViewById(R.id.layoutOwner)
        val avatar: ImageView = itemView.findViewById(R.id.member_avatar)
        val imageProfile: ImageView = itemView.findViewById(R.id.imgProfile)
        val txtRole: TextView = itemView.findViewById(R.id.txtRole)
    }
}
