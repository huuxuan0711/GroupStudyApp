package com.xmobile.project1groupstudyappnew.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.member.MemberProgress
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader

class LegendMemberAdapter(
    val members: List<MemberProgress>
): RecyclerView.Adapter<LegendMemberAdapter.ViewHolder>() {
    var filterList = members

    fun setFilter(list: List<MemberProgress>){
        filterList = list
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_member_progress, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.name.text = filterList[position].memberName
        holder.doneCount.text = filterList[position].doneCount.toString()
        holder.totalCount.text = filterList[position].totalCount.toString()

        AvatarLoader.load(holder.itemView.context, holder.avatar, filterList[position].avatarUrl, filterList[position].memberName)
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.member_avatar)
        val name: TextView = itemView.findViewById(R.id.member_name)
        val doneCount: TextView = itemView.findViewById(R.id.doneCount)
        val totalCount: TextView = itemView.findViewById(R.id.totalCount)
    }
}