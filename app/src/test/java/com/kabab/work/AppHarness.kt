package com.kabab.work

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kabab.work.data.Priority
import com.kabab.work.data.Project
import com.kabab.work.data.Task
import com.kabab.work.data.WorkDatabase
import com.kabab.work.ui.WorkOrganizerApp
import com.kabab.work.ui.WorkViewModel
import com.kabab.work.ui.theme.WorkTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId

/** Runs the real UI against an in-memory database with a fixed date and clock. */
class AppHarness(private val compose: ComposeContentTestRule) {
    val today: LocalDate = LocalDate.of(2026, 11, 12)
    val noon: Long = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WorkDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val dao = db.workDao()

    fun close() = db.close()

    fun launch(darkTheme: Boolean = false) {
        val viewModel = WorkViewModel(dao, now = { noon })
        compose.setContent {
            WorkTheme(darkTheme = darkTheme, dynamicColor = false) { WorkOrganizerApp(viewModel, today) }
        }
    }

    fun waitForText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    fun waitForNoText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }

    fun savedTasks(): List<Task> = runBlocking { dao.observeTasks().first() }

    fun savedProjects(): List<Project> = runBlocking { dao.observeProjects().first() }

    fun addTask(task: Task): Unit = runBlocking { dao.upsertTask(task) }

    /** A realistic week of work: three projects and tasks in every state. */
    fun seed(): Unit = runBlocking {
        val website = dao.upsertProject(Project(name = "Website relaunch", colorIndex = 0))
        val hiring = dao.upsertProject(Project(name = "Hiring", colorIndex = 1))
        dao.upsertProject(Project(name = "Q4 planning", colorIndex = 2))
        val day = today.toEpochDay()
        listOf(
            Task(title = "Send invoice to Acme", dueEpochDay = day - 2, priority = Priority.HIGH.level),
            Task(title = "Review homepage copy", dueEpochDay = day, projectId = website, priority = Priority.HIGH.level),
            Task(title = "Reply to candidate emails", dueEpochDay = day, projectId = hiring),
            Task(title = "Book meeting room", dueEpochDay = day, done = true, completedAt = noon),
            Task(title = "Prepare interview questions", dueEpochDay = day + 1, projectId = hiring),
            Task(title = "Fix checkout bug", dueEpochDay = day + 4, projectId = website),
            Task(title = "Update team wiki", projectId = website, done = true, completedAt = noon - 86_400_000L * 3),
            Task(title = "Clean up shared drive", priority = Priority.LOW.level),
        ).forEachIndexed { index, task -> dao.upsertTask(task.copy(createdAt = index.toLong())) }
    }
}
