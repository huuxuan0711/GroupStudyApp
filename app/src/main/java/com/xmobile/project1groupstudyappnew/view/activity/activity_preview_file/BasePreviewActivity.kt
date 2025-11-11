package com.xmobile.project1groupstudyappnew.view.activity.activity_preview_file

import android.content.Context
import android.widget.ImageView
import android.widget.Toast
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.viewmodel.FileViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import com.xmobile.project1groupstudyappnew.view.activity.BaseActivity

@AndroidEntryPoint
open class BasePreviewActivity : BaseActivity() {

    val viewModel: FileViewModel by viewModels()
    var absolutePath: String? = null

    fun setUpButton(
        btnClose: ImageView,
        btnDownload: ImageView,
        btnShare: ImageView,
        file: File?,
        context: Context
    ) {
        // Close button
        btnClose.setOnClickListener { finish() }

        // Download button
        btnDownload.setOnClickListener {
            file?.let { f ->
                absolutePath?.let { path ->
                    viewModel.downloadFile(f, path)
                } ?: run {
                    Toast.makeText(context, "File chưa sẵn sàng để tải", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(context, "File không hợp lệ", Toast.LENGTH_SHORT).show()
            }
        }

        // Share button
        btnShare.setOnClickListener {
            file?.let { f ->
                viewModel.shareFile(f, context)
            } ?: run {
                Toast.makeText(context, "File không hợp lệ", Toast.LENGTH_SHORT).show()
            }
        }

        // Download to cache ngay khi setup
        file?.url?.takeIf { it.isNotBlank() }?.let { url ->
            viewModel.downloadFileToCache(url, context)
        }
    }
}
