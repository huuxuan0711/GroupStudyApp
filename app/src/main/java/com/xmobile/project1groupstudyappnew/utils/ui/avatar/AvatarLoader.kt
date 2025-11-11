package com.xmobile.project1groupstudyappnew.utils.ui.avatar

import android.content.Context
import android.widget.ImageView
import com.avatarfirst.avatargenlib.AvatarGenerator
import com.bumptech.glide.Glide
import com.xmobile.project1groupstudyappnew.utils.RandomColor

object AvatarLoader {
    fun load(
        context: Context,
        imageView: ImageView,
        url: String?,
        label: String
    ) {
        if (!url.isNullOrEmpty()) {
            Glide.with(context).load(url).into(imageView)
        } else {
            val drawable = AvatarGenerator.AvatarBuilder(context)
                .setLabel(label)
                .setAvatarSize(120)
                .setTextSize(30)
                .toSquare()
                .setBackgroundColor(RandomColor.getRandomColor())
                .build()
            imageView.setImageDrawable(drawable)
        }
    }
}