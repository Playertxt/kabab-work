package com.kabab.work

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kabab.work.data.Project
import com.kabab.work.data.Task
import com.kabab.work.data.WorkDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkDaoTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WorkDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.workDao()

    @After
    fun tearDown() = db.close()

    @Test
    fun `upsert inserts then updates the same row`() = runBlocking {
        val id = dao.upsertTask(Task(title = "Draft report"))
        dao.upsertTask(Task(id = id, title = "Draft report v2", done = true, completedAt = 5))

        val task = dao.observeTasks().first().single()
        assertEquals(id, task.id)
        assertEquals("Draft report v2", task.title)
        assertEquals(true, task.done)
    }

    @Test
    fun `deleting a project keeps its tasks and unassigns them`() = runBlocking {
        val projectId = dao.upsertProject(Project(name = "Website"))
        dao.upsertTask(Task(title = "Fix header", projectId = projectId))

        dao.deleteProject(Project(id = projectId, name = "Website"))

        assertEquals(emptyList<Project>(), dao.observeProjects().first())
        assertNull(dao.observeTasks().first().single().projectId)
    }

    @Test
    fun `clearing completed tasks leaves open ones`() = runBlocking {
        dao.upsertTask(Task(title = "open"))
        dao.upsertTask(Task(title = "done", done = true, completedAt = 1))
        dao.upsertTask(Task(title = "also done", done = true, completedAt = 2))

        assertEquals(2, dao.deleteCompletedTasks())
        assertEquals(listOf("open"), dao.observeTasks().first().map { it.title })
    }

    @Test
    fun `projects are listed alphabetically ignoring case`() = runBlocking {
        listOf("zeta", "Alpha", "beta").forEach { dao.upsertProject(Project(name = it)) }
        assertEquals(listOf("Alpha", "beta", "zeta"), dao.observeProjects().first().map { it.name })
    }
}
