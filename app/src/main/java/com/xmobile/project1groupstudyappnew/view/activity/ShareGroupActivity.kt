package com.xmobile.project1groupstudyappnew.view.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.avatarfirst.avatargenlib.AvatarGenerator
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.xmobile.project1groupstudyappnew.databinding.ActivityShareGroupBinding
import com.xmobile.project1groupstudyappnew.model.obj.group.Group
import com.xmobile.project1groupstudyappnew.utils.RandomColor
import com.xmobile.project1groupstudyappnew.utils.ui.avatar.AvatarLoader

class ShareGroupActivity : BaseActivity() {

    private lateinit var binding: ActivityShareGroupBinding
    private lateinit var group: Group
    private lateinit var deeplink: String
    private lateinit var linkShare: String

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initControl()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initControl() {
        getData()

        binding.backLayout.setOnClickListener { finish() }

        binding.imgCopy.setOnClickListener { copyToClipboard(group.inviteCode) }
        binding.txtLinkGroup.setOnClickListener { copyToClipboard(deeplink) }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("DeepLink", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Đã copy link vào clipboard", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getData() {
        group = intent.getSerializableExtra("group", Group::class.java) as Group
        deeplink = "studyhub://invite?inviteCode=${group.inviteCode}"
        linkShare = "studyhub://invite/${group.inviteCode}"
        displayData()
    }

    private fun displayData() {
        // Hiển thị avatar nhóm
        AvatarLoader.load(this, binding.groupImage, group.avatar, group.name)

        binding.groupName.text = group.name
        binding.groupId.text = group.inviteCode
        binding.txtLinkGroup.text = linkShare

        // Tạo QR code
        generateQRCode(deeplink)
    }

    private fun generateQRCode(data: String) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        binding.imgQR.setImageBitmap(bmp)
    }
}
