package com.kabab.work

import com.kabab.work.data.Priority
import com.kabab.work.data.Project
import com.kabab.work.data.Task
import com.kabab.work.domain.NO_PROJECT
import com.kabab.work.domain.StatusFilter
import com.kabab.work.domain.bucketForToday
import com.kabab.work.domain.dueLabel
import com.kabab.work.domain.filterTasks
import com.kabab.work.domain.isOverdue
import com.kabab.work.domain.projectStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class TaskLogicTest {
    private val today = LocalDate.of(2026, 11, 12) // a Thursday
    private val zone = ZoneId.of("UTC")
    private var nextId = 1L

    private fun task(
        title: String,
        dueInDays: Long? = null,
        priority: Priority = Priority.NORMAL,
        done: Boolean = false,
        completedOn: LocalDate? = if (done) today else null,
        projectId: Long? = null,
        notes: String = "",
    ) = Task(
        id = nextId++,
        title = title,
        notes = notes,
        projectId = projectId,
        priority = priority.level,
        dueEpochDay = dueInDays?.let { today.toEpochDay() + it },
        done = done,
        completedAt = completedOn?.atTime(12, 0)?.atZone(zone)?.toInstant()?.toEpochMilli(),
        createdAt = nextId,
    )

    @Test
    fun `today buckets split tasks by due date`() {
        val tasks = listOf(
            task("late", dueInDays = -2),
            task("now", dueInDays = 0),
            task("soon", dueInDays = 7),
            task("far", dueInDays = 8),
            task("undated"),
            task("finished today", dueInDays = -1, done = true),
            task("finished yesterday", dueInDays = 0, done = true, completedOn = today.minusDays(1)),
        )

        val buckets = bucketForToday(tasks, today, zone)

        assertEquals(listOf("late"), buckets.overdue.map { it.title })
        assertEquals(listOf("now"), buckets.dueToday.map { it.title })
        assertEquals(listOf("soon"), buckets.upcoming.map { it.title })
        assertEquals(listOf("finished today"), buckets.completedToday.map { it.title })
        assertEquals(3, buckets.plannedCount)
        assertFalse(buckets.isEmpty)
    }

    @Test
    fun `buckets are empty when nothing is scheduled`() {
        assertTrue(bucketForToday(listOf(task("undated"), task("far", dueInDays = 30)), today, zone).isEmpty)
    }

    @Test
    fun `tasks due the same day are ordered by priority`() {
        val tasks = listOf(
            task("low", dueInDays = 0, priority = Priority.LOW),
            task("high", dueInDays = 0, priority = Priority.HIGH),
            task("normal", dueInDays = 0),
        )
        assertEquals(listOf("high", "normal", "low"), bucketForToday(tasks, today, zone).dueToday.map { it.title })
    }

    @Test
    fun `filter sorts open tasks by due date with undated and done last`() {
        val tasks = listOf(
            task("undated high", priority = Priority.HIGH),
            task("done", dueInDays = -5, done = true),
            task("next week", dueInDays = 6),
            task("overdue", dueInDays = -1),
        )
        assertEquals(
            listOf("overdue", "next week", "undated high", "done"),
            filterTasks(tasks).map { it.title },
        )
    }

    @Test
    fun `filter by status`() {
        val tasks = listOf(task("open"), task("done", done = true))
        assertEquals(listOf("open"), filterTasks(tasks, status = StatusFilter.OPEN).map { it.title })
        assertEquals(listOf("done"), filterTasks(tasks, status = StatusFilter.DONE).map { it.title })
        assertEquals(2, filterTasks(tasks, status = StatusFilter.ALL).size)
    }

    @Test
    fun `filter by project and by no project`() {
        val tasks = listOf(task("a", projectId = 1), task("b", projectId = 2), task("loose"))
        assertEquals(listOf("a"), filterTasks(tasks, projectFilter = 1).map { it.title })
        assertEquals(listOf("loose"), filterTasks(tasks, projectFilter = NO_PROJECT).map { it.title })
        assertEquals(3, filterTasks(tasks, projectFilter = null).size)
    }

    @Test
    fun `search matches title and notes ignoring case`() {
        val tasks = listOf(
            task("Send INVOICE"),
            task("Call supplier", notes = "ask about the invoice"),
            task("Lunch"),
        )
        assertEquals(
            listOf("Send INVOICE", "Call supplier"),
            filterTasks(tasks, query = "  invoice ").map { it.title },
        )
    }

    @Test
    fun `project stats count open and done tasks`() {
        val projects = listOf(Project(id = 1, name = "Website"), Project(id = 2, name = "Empty"))
        val tasks = listOf(
            task("a", projectId = 1),
            task("b", projectId = 1, done = true),
            task("c", projectId = 1, done = true),
            task("other"),
        )

        val (website, empty) = projectStats(projects, tasks)

        assertEquals(1, website.open)
        assertEquals(2, website.done)
        assertEquals(2f / 3f, website.progress, 0.0001f)
        assertEquals(0, empty.total)
        assertEquals(0f, empty.progress, 0f)
    }

    @Test
    fun `only open tasks with a past due date are overdue`() {
        assertTrue(isOverdue(task("late", dueInDays = -1), today))
        assertFalse(isOverdue(task("today", dueInDays = 0), today))
        assertFalse(isOverdue(task("done late", dueInDays = -1, done = true), today))
        assertFalse(isOverdue(task("undated"), today))
    }

    @Test
    fun `due labels are relative near today and dated otherwise`() {
        assertEquals("Today", dueLabel(today, today, Locale.UK))
        assertEquals("Tomorrow", dueLabel(today.plusDays(1), today, Locale.UK))
        assertEquals("Yesterday", dueLabel(today.minusDays(1), today, Locale.UK))
        assertEquals("Mon, 16 Nov", dueLabel(LocalDate.of(2026, 11, 16), today, Locale.UK))
        assertEquals("4 Jan 2027", dueLabel(LocalDate.of(2027, 1, 4), today, Locale.UK))
    }
}
