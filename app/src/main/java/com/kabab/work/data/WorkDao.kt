package com.kabab.work.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Query("SELECT * FROM tasks")
    fun observeTasks(): Flow<List<Task>>

    @Query("SELECT * FROM projects ORDER BY name COLLATE NOCASE")
    fun observeProjects(): Flow<List<Project>>

    @Upsert
    suspend fun upsertTask(task: Task): Long

    @Upsert
    suspend fun upsertProject(project: Project): Long

    @Delete
    suspend fun deleteTask(task: Task)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM tasks WHERE done = 1")
    suspend fun deleteCompletedTasks(): Int
}
