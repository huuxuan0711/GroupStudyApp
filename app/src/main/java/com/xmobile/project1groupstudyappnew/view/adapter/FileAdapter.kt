package com.xmobile.project1groupstudyappnew.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.model.obj.file.File

class FileAdapter(
    private val onItemClick: (File) -> Unit,
    private val onItemLongClick: (File, View) -> Unit
) : ListAdapter<File, FileAdapter.FileViewHolder>(DiffCallback()) {

    private var allFiles: List<File> = emptyList()

    class DiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean = oldItem == newItem
    }

    fun submitAllFiles(files: List<File>) {
        allFiles = files
        submitList(files.filter { !it.inTask })
    }

    fun filterByType(type: Int) {
        val filtered = if (type == -1) {
            allFiles.filter { !it.inTask }
        } else {
            allFiles.filter { it.type == type && !it.inTask }
        }
        submitList(filtered)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_file_list, parent, false))
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = getItem(position)
        holder.apply {
            fileCategory.setImageResource(getDrawable(file.type))
            fileName.text = file.name
            memberName.text = file.uploadedByName
            fileCapacity.text = getCapacity(file.capacity)
            Glide.with(itemView).load(file.previewUrl).into(fileThumbnail)

            itemView.setOnClickListener { onItemClick(file) }
            itemView.setOnLongClickListener { v ->
                onItemLongClick(file, v)
                true
            }
        }
    }

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileCategory: ImageView = itemView.findViewById(R.id.image_file_category)
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val fileCapacity: TextView = itemView.findViewById(R.id.file_capacity)
        val fileThumbnail: ImageView = itemView.findViewById(R.id.file_thumbnail)
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

    private fun getDrawable(type: Int): Int = when (type) {
        0 -> R.drawable.ic_pdf
        1 -> R.drawable.ic_image
        2 -> R.drawable.ic_video
        else -> R.drawable.ic_file
    }
}
