package com.xmobile.project1groupstudyappnew.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.fragment.app.Fragment
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.viewmodel.FileViewModel

class UploadFileHandler(
    private val caller: ActivityResultCaller,
    private val context: Context,
    private val fileViewModel: FileViewModel
) {

    private var progressBar: ProgressBar? = null

    private lateinit var pickFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var legacyLauncher: ActivityResultLauncher<Intent>

    fun initLaunchers() {

        pickFileLauncher = caller.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri -> uri?.let { handleUriUpload(it) } }

        pickMedia = caller.registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri -> uri?.let { handleUriUpload(it) } }

        legacyLauncher = caller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result -> if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let { handleUriUpload(it) } }
    }

    @SuppressLint("RestrictedApi")
    fun uploadFile(progressBar: ProgressBar, anchorView: View) {
        this.progressBar = progressBar

        val menu = MenuBuilder(context)
        val inflater = when (caller) {
            is Fragment -> caller.requireActivity().menuInflater
            is ComponentActivity -> caller.menuInflater
            else -> return
        }
        inflater.inflate(R.menu.menu_attach_file, menu)

        val popup = MenuPopupHelper(context, menu, anchorView)
        popup.gravity = Gravity.CENTER
        popup.setForceShowIcon(true)

        menu.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.menu_image_gallery -> openGallery()
                    R.id.menu_select_file -> pickFileLauncher.launch(arrayOf("*/*"))
                }
                return true
            }
            override fun onMenuModeChange(menu: MenuBuilder) {}
        })

        popup.show()
    }

    private fun openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        } else {
            legacyLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
        }
    }

    private fun handleUriUpload(uri: Uri) {
        try {
            // Giữ quyền truy cập với SAF / MediaStore
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }

            showProgress()

            fileViewModel.setUriUpload(uri)

        } catch (_: Exception) {
            Toast.makeText(context, "Lỗi xử lý file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showProgress() {
        progressBar?.visibility = View.VISIBLE
        disableUserInteraction()
    }

    fun disableUserInteraction() {
        val window = when (caller) {
            is Fragment -> caller.requireActivity().window
            is ComponentActivity -> caller.window
            else -> return
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun enableUserInteraction() {
        val window = when (caller) {
            is Fragment -> caller.requireActivity().window
            is ComponentActivity -> caller.window
            else -> return
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}
