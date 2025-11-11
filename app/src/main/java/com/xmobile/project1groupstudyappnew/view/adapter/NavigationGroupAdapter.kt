package com.xmobile.project1groupstudyappnew.view.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R

class NavigationGroupAdapter (
    private val list: List<String>,
    private val context: Context,
    private val onItemClick: (Int) -> Unit
): RecyclerView.Adapter<NavigationGroupAdapter.ViewHolder>() {

    private var selectedItem = 0

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedItem(position: Int) {
        selectedItem = position
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        itemView: View
    ): RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.txtCategory)!!
        val icon = itemView.findViewById<ImageView>(R.id.imgCategory)!!
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_navigation, parent, false))
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (selectedItem == position) {
            holder.itemView.setBackgroundResource(R.drawable.bg_blue)
            holder.title.setTextColor(context.resources.getColor(R.color.white))
            holder.icon.setColorFilter(context.resources.getColor(R.color.white))
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_gray)
            holder.title.setTextColor(context.resources.getColor(R.color.gray))
            holder.icon.setColorFilter(context.resources.getColor(R.color.gray))
        }

        holder.title.text = list[position]

        if (list[position] == context.getString(R.string.task_study)) {
            holder.icon.setImageResource(R.drawable.ic_task)
        } else if (list[position] == context.getString(R.string.task_project)) {
            holder.icon.setImageResource(R.drawable.ic_task)
        }else if (list[position] == context.getString(R.string.chat)) {
            holder.icon.setImageResource(R.drawable.ic_chat)
        }else if (list[position] == context.getString(R.string.calendar)) {
            holder.icon.setImageResource(R.drawable.ic_calendar)
        }else if (list[position] == context.getString(R.string.progress)) {
            holder.icon.setImageResource(R.drawable.ic_progress)
        }else if (list[position] == context.getString(R.string.document)) {
            holder.icon.setImageResource(R.drawable.ic_document)
        }

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}