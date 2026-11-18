package com.kabab.work.domain

import com.kabab.work.data.Project
import com.kabab.work.data.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Open tasks: soonest due date first (undated last), then highest priority, then oldest. */
val byUrgency: Comparator<Task> =
    compareBy<Task> { it.dueEpochDay ?: Long.MAX_VALUE }
        .thenByDescending { it.priority }
        .thenBy { it.createdAt }
        .thenBy { it.id }

/** Finished tasks: most recently completed first. */
val byRecentlyCompleted: Comparator<Task> =
    compareByDescending<Task> { it.completedAt ?: 0L }.thenBy { it.id }

data class TodayBuckets(
    val overdue: List<Task>,
    val dueToday: List<Task>,
    val upcoming: List<Task>,
    val completedToday: List<Task>,
) {
    /** Everything that was on today's plate: still-open overdue and due-today tasks plus what got done. */
    val plannedCount: Int get() = overdue.size + dueToday.size + completedToday.size
    val isEmpty: Boolean
        get() = overdue.isEmpty() && dueToday.isEmpty() && upcoming.isEmpty() && completedToday.isEmpty()
}

const val UPCOMING_DAYS = 7L

fun bucketForToday(
    tasks: List<Task>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): TodayBuckets {
    val todayDay = today.toEpochDay()
    val open = tasks.filter { !it.done }
    return TodayBuckets(
        overdue = open.filter { it.dueEpochDay != null && it.dueEpochDay < todayDay }.sortedWith(byUrgency),
        dueToday = open.filter { it.dueEpochDay == todayDay }.sortedWith(byUrgency),
        upcoming = open
            .filter { it.dueEpochDay != null && it.dueEpochDay > todayDay && it.dueEpochDay <= todayDay + UPCOMING_DAYS }
            .sortedWith(byUrgency),
        completedToday = tasks
            .filter { task ->
                task.done && task.completedAt != null &&
                    Instant.ofEpochMilli(task.completedAt).atZone(zone).toLocalDate() == today
            }
            .sortedWith(byRecentlyCompleted),
    )
}

enum class StatusFilter(val label: String) { OPEN("Open"), DONE("Done"), ALL("All") }

/** Project filter value meaning "only tasks that are not in any project". */
const val NO_PROJECT: Long = -1L

/**
 * @param projectFilter null for every project, [NO_PROJECT] for unassigned tasks, otherwise a project id.
 */
fun filterTasks(
    tasks: List<Task>,
    query: String = "",
    status: StatusFilter = StatusFilter.ALL,
    projectFilter: Long? = null,
): List<Task> {
    val needle = query.trim()
    val matching = tasks.filter { task ->
        val statusOk = when (status) {
            StatusFilter.OPEN -> !task.done
            StatusFilter.DONE -> task.done
            StatusFilter.ALL -> true
        }
        val projectOk = when (projectFilter) {
            null -> true
            NO_PROJECT -> task.projectId == null
            else -> task.projectId == projectFilter
        }
        val queryOk = needle.isEmpty() ||
            task.title.contains(needle, ignoreCase = true) ||
            task.notes.contains(needle, ignoreCase = true)
        statusOk && projectOk && queryOk
    }
    return matching.filter { !it.done }.sortedWith(byUrgency) +
        matching.filter { it.done }.sortedWith(byRecentlyCompleted)
}

data class ProjectStats(val project: Project, val open: Int, val done: Int) {
    val total: Int get() = open + done
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
}

fun projectStats(projects: List<Project>, tasks: List<Task>): List<ProjectStats> {
    val byProject = tasks.groupBy { it.projectId }
    return projects.map { project ->
        val own = byProject[project.id].orEmpty()
        val done = own.count { it.done }
        ProjectStats(project, open = own.size - done, done = done)
    }
}

fun isOverdue(task: Task, today: LocalDate): Boolean =
    !task.done && task.dueEpochDay != null && task.dueEpochDay < today.toEpochDay()

/** "Today", "Tomorrow", "Yesterday", otherwise a short date (with the year only when it differs). */
fun dueLabel(due: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String =
    when (due.toEpochDay() - today.toEpochDay()) {
        0L -> "Today"
        1L -> "Tomorrow"
        -1L -> "Yesterday"
        else -> {
            val pattern = if (due.year == today.year) "EEE, d MMM" else "d MMM yyyy"
            due.format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }
