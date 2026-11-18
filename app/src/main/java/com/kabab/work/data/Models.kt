package com.kabab.work.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Priority(val level: Int, val label: String) {
    LOW(0, "Low"),
    NORMAL(1, "Normal"),
    HIGH(2, "High");

    companion object {
        fun fromLevel(level: Int): Priority = entries.firstOrNull { it.level == level } ?: NORMAL
    }
}

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Index into the project colour palette. */
    val colorIndex: Int = 0,
    val createdAt: Long = 0,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            // Deleting a project keeps its tasks; they just become unassigned.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("projectId")],
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val projectId: Long? = null,
    /** A [Priority.level]. */
    val priority: Int = Priority.NORMAL.level,
    /** Due date as days since 1970-01-01, or null when the task has no due date. */
    val dueEpochDay: Long? = null,
    val done: Boolean = false,
    /** Epoch millis when the task was ticked off; null while it is open. */
    val completedAt: Long? = null,
    val createdAt: Long = 0,
)
