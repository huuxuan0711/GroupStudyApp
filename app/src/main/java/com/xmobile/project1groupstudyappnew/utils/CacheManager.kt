package com.xmobile.project1groupstudyappnew.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object CacheManager {
    // Xóa các file cache cũ hơn `maxAgeHours` (mặc định: 24h)
    fun clearOldCache(context: Context, maxAgeHours: Long = 24) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val maxAgeMillis = maxAgeHours * 3600_000
                val cacheDir: File = context.cacheDir

                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile && now - file.lastModified() > maxAgeMillis) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}