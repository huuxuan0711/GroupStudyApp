package com.xmobile.project1groupstudyappnew.view.adapter

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R

class LegendGroupAdapter(
    val list: List<Pair<Int, Int>>,
    val context: android.content.Context
): RecyclerView.Adapter<LegendGroupAdapter.ViewHolder>() {
    val total = list.sumOf { it.second }
    var filterList = list

    fun setFilter(list: List<Pair<Int, Int>>){
        filterList = list
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_progress_group, parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = filterList[position]
        when(item.first){
            0 -> {
                holder.statusLegend.setColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
                holder.statusName.text = context.getString(R.string.to_do)
                holder.statusCount.text = item.second.toString()
            }
            1 -> {
                holder.statusLegend.setColorFilter(ContextCompat.getColor(context, R.color.yellow), PorterDuff.Mode.SRC_IN)
                holder.statusName.text = context.getString(R.string.in_progress)
                holder.statusCount.text = item.second.toString()
            }
            3 -> {
                holder.statusLegend.setColorFilter(ContextCompat.getColor(context, R.color.green), PorterDuff.Mode.SRC_IN)
                holder.statusName.text = context.getString(R.string.done)
                holder.statusCount.text = item.second.toString()
            }
        }
        holder.totalCount.text = total.toString()
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val statusLegend: ImageView = itemView.findViewById(R.id.statusLegend)
        val statusName: TextView = itemView.findViewById(R.id.statusName)
        val statusCount: TextView = itemView.findViewById(R.id.statusCount)
        val totalCount: TextView = itemView.findViewById(R.id.totalCount)
    }
}