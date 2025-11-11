package com.xmobile.project1groupstudyappnew.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xmobile.project1groupstudyappnew.room.dao.FileDao
import com.xmobile.project1groupstudyappnew.room.entity.FileRoom

@Database(entities = [FileRoom::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun fileDao(): FileDao
}