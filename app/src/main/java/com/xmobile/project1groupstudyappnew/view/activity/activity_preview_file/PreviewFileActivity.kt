package com.xmobile.project1groupstudyappnew.view.activity.activity_preview_file

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.databinding.ActivityPreviewFileBinding
import com.xmobile.project1groupstudyappnew.model.obj.file.File
import com.xmobile.project1groupstudyappnew.model.state.FileUIState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PreviewFileActivity : BasePreviewActivity() {

    private lateinit var binding: ActivityPreviewFileBinding
    private var file: File? = null
    private var groupId: String = ""
    private var userId: String = ""
    private var player: ExoPlayer? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewFileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initControl()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initControl() {
        getData()
        setUpButton(binding.btnClose, binding.btnDownload, binding.btnShare, file, this)
        collectState()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getData() {
        val intent = intent
        file = intent.getSerializableExtra("file", File::class.java) as? File
        if (file == null) {
            Toast.makeText(this, "File không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        groupId = intent.getStringExtra("groupId") ?: ""
        userId = intent.getStringExtra("userId") ?: ""
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fileState.collect { state ->
                    when (state) {
                        is FileUIState.Error -> {
                            Toast.makeText(this@PreviewFileActivity, state.message, Toast.LENGTH_SHORT).show()
                            showLoading(false)
                        }
                        FileUIState.Loading -> showLoading(true)
                        is FileUIState.SuccessDownloadFile -> {
                            showLoading(false)
                            Toast.makeText(this@PreviewFileActivity, getString(R.string.download_success), Toast.LENGTH_SHORT).show()
                        }
                        is FileUIState.SuccessDownloadToCache -> {
                            showLoading(false)
                            state.file?.let { f ->
                                absolutePath = f.absolutePath
                                previewFile(f)
                            }
                        }
                        is FileUIState.SuccessGetFileRaw -> {
                            showLoading(false)
                            proceedShare(state.filePath)
                        }
                        is FileUIState.SuccessGetFileFromCache -> {
                            showLoading(false)
                            proceedShare(state.absolutePath)
                        }
                        is FileUIState.SuccessGetFileUrl -> shareLink(state.url)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        val visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.btnDownload.visibility = visibility
        binding.btnShare.visibility = visibility
    }

    private fun previewFile(localFile: java.io.File) {
        if (!localFile.exists()) {
            Toast.makeText(this, "File không tồn tại trong cache.", Toast.LENGTH_SHORT).show()
            return
        }

        val format = file!!.name.substringAfterLast(".").lowercase()
        when (format) {
            "pdf" -> previewPdf(localFile)
            "jpg", "jpeg", "png", "webp", "gif" -> previewImage(localFile)
            "mp4", "webm" -> previewVideo(localFile)
            "mp3", "wav", "aac" -> previewAudio(localFile)
            "txt", "json", "csv" -> previewText(localFile)
            else -> preViewOther()
        }
    }

    private fun preViewOther() {
        showViewer(other = true)
    }

    // PDF
    private fun previewPdf(file: java.io.File) {
        showViewer(pdf = true)

        binding.pdfViewer.fromFile(file)
            .defaultPage(0)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAnnotationRendering(true)
            .spacing(8)
            .scrollHandle(DefaultScrollHandle(this))
            .onError { t -> Toast.makeText(this, "Lỗi PDF: ${t.message}", Toast.LENGTH_SHORT).show() }
            .onPageError { page, t -> Toast.makeText(this, "Lỗi trang $page: ${t.message}", Toast.LENGTH_SHORT).show() }
            .onLoad { Toast.makeText(this, "Đã tải xong PDF (${file.name})", Toast.LENGTH_SHORT).show() }
            .load()
    }

    // Image
    private fun previewImage(file: java.io.File) {
        showViewer(image = true)
        Glide.with(this).load(file).into(binding.imageViewer)
    }

    // Video
    private fun previewVideo(file: java.io.File) {
        showViewer(video = true)

        player?.release()
        player = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            setMediaItem(mediaItem)
            prepare()
        }
        binding.videoViewer.player = player
    }

    // Audio
    private fun previewAudio(file: java.io.File) {
        showViewer(audio = true)

        player?.release()
        player = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            setMediaItem(mediaItem)
            prepare()
        }
        binding.audioViewer.player = player
    }

    // Text
    private fun previewText(file: java.io.File) {
        showViewer(text = true)

        lifecycleScope.launch(Dispatchers.IO) {
            val content = file.bufferedReader().use { it.readText() }
            withContext(Dispatchers.Main) {
                binding.textViewer.text = content
            }
        }
    }

    private fun proceedShare(filePath: String?) {
        if (filePath.isNullOrEmpty()) return
        val localFile = java.io.File(filePath)
        if (!localFile.exists()) return

        val fileUri = try {
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", localFile)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Không thể chia sẻ file.", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = when (file?.type) {
                0 -> "application/pdf"
                1 -> "image/*"
                2 -> "video/*"
                else -> "*/*"
            }
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Chia sẻ tệp qua"))
    }

    private fun shareLink(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ liên kết"))
    }

    private fun showViewer(pdf: Boolean = false, image: Boolean = false, video: Boolean = false,
                           audio: Boolean = false, text: Boolean = false, other: Boolean = false) {
        binding.pdfViewer.visibility = if (pdf) View.VISIBLE else View.GONE
        binding.imageViewer.visibility = if (image) View.VISIBLE else View.GONE
        binding.videoViewer.visibility = if (video) View.VISIBLE else View.GONE
        binding.audioViewer.visibility = if (audio) View.VISIBLE else View.GONE
        binding.textViewer.visibility = if (text) View.VISIBLE else View.GONE
        binding.otherViewer.visibility = if (other) View.VISIBLE else View.GONE
    }


    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
