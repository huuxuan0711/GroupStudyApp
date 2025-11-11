package com.xmobile.project1groupstudyappnew.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader

class GroupAdapter(
    private val context: Context,
    private val onItemClick: (Group) -> Unit
) : ListAdapter<Group, GroupAdapter.ViewHolder>(DiffCallback()) {

    private var latestMessageMap: Map<String, String> = emptyMap()
    private var lastSeenMessageMap: Map<String, String> = emptyMap()

    class DiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.groupId == newItem.groupId
        }

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem == newItem
        }
    }

    fun submitGroups(
        groups: List<Group>,
        latestMap: Map<String, String>,
        lastSeenMap: Map<String, String>
    ) {
        latestMessageMap = latestMap
        lastSeenMessageMap = lastSeenMap
        submitList(groups)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_group_list, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = getItem(position)
        holder.name.text = group.name
        holder.category.text = if (group.type == 1) context.getString(R.string.study)
        else context.getString(R.string.project)
        holder.size.text = group.size.toString()

        val latestId = latestMessageMap[group.groupId]
        val lastSeenId = lastSeenMessageMap[group.groupId]
        holder.layoutNewInfo.visibility =
            if (latestId != null && latestId != lastSeenId) View.VISIBLE else View.GONE
        holder.newInfo.text = "Tin mới"

        AvatarLoader.load(context, holder.avatar, group.avatar, group.name)

        holder.itemView.setOnClickListener {
            onItemClick(group)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.group_name)
        val category: TextView = itemView.findViewById(R.id.group_category)
        val size: TextView = itemView.findViewById(R.id.group_size)
        val newInfo: TextView = itemView.findViewById(R.id.group_new_info)
        val layoutNewInfo: ViewGroup = itemView.findViewById(R.id.layoutNewInfo)
        val avatar: ImageView = itemView.findViewById(R.id.group_image)
    }
}
