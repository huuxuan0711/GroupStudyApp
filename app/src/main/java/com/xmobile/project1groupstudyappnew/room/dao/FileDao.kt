package com.xmobile.project1groupstudyappnew.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xmobile.project1groupstudyappnew.room.entity.FileRoom

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileRoom)

    @Query("SELECT * FROM files WHERE fileId = :id LIMIT 1")
    suspend fun getFileById(id: String): FileRoom?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFile(file: FileRoom)

    @Query("SELECT * FROM files WHERE url = :url LIMIT 1")
    suspend fun getFileByUrl(url: String): FileRoom?

    @Query("UPDATE files SET filePath = :path, status = :status WHERE fileId = :id")
    suspend fun updateFilePath(id: String, path: String?, status: Int)

    @Delete
    suspend fun deleteFile(file: FileRoom)
}