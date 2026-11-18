package com.kabab.work.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class, Project::class], version = 1, exportSchema = false)
abstract class WorkDatabase : RoomDatabase() {
    abstract fun workDao(): WorkDao

    companion object {
        fun create(context: Context): WorkDatabase =
            Room.databaseBuilder(context, WorkDatabase::class.java, "work.db").build()
    }
}
