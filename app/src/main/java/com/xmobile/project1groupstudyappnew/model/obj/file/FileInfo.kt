package com.xmobile.project1groupstudyappnew.model.obj.file

import java.io.InputStream

data class FileInfo(
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val inputStream: InputStream?
)
