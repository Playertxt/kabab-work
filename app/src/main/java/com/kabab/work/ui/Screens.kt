package com.kabab.work.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kabab.work.R
import com.kabab.work.data.Project
import com.kabab.work.data.Task
import com.kabab.work.domain.NO_PROJECT
import com.kabab.work.domain.ProjectStats
import com.kabab.work.domain.StatusFilter
import com.kabab.work.domain.TodayBuckets
import com.kabab.work.ui.theme.projectColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Leaves room at the end of a list so the floating button never covers the last row. */
private val ListPadding = PaddingValues(bottom = 96.dp)

@Composable
fun TodayScreen(
    buckets: TodayBuckets,
    projectsById: Map<Long, Project>,
    today: LocalDate,
    onToggle: (Task, Boolean) -> Unit,
    onOpen: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = ListPadding) {
        item(key = "summary") { TodaySummary(buckets, today) }

        if (buckets.isEmpty) {
            item(key = "empty") {
                EmptyState(
                    title = "Nothing scheduled",
                    body = "Tasks due today or in the next week show up here. Tap New task to plan your day.",
                    modifier = Modifier.padding(top = 48.dp),
                )
            }
        }

        fun section(title: String, tasks: List<Task>, isError: Boolean = false) {
            if (tasks.isEmpty()) return
            item(key = "header-$title") {
                SectionHeader(
                    title = title,
                    count = tasks.size,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.animateItem(),
                )
            }
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    project = task.projectId?.let(projectsById::get),
                    today = today,
                    onToggle = { onToggle(task, it) },
                    onClick = { onOpen(task) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        section("Overdue", buckets.overdue, isError = true)
        section("Today", buckets.dueToday)
        section("Next 7 days", buckets.upcoming)
        section("Completed today", buckets.completedToday)
    }
}

@Composable
private fun TodaySummary(buckets: TodayBuckets, today: LocalDate) {
    val done = buckets.completedToday.size
    val planned = buckets.plannedCount
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (planned > 0) {
            LinearProgressIndicator(
                progress = { done.toFloat() / planned },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            val left = planned - done
            Text(
                text = if (left == 0) "All $planned done for today" else "$done of $planned done · $left to go",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TasksScreen(
    tasks: List<Task>,
    projects: List<Project>,
    today: LocalDate,
    query: String,
    onQueryChange: (String) -> Unit,
    status: StatusFilter,
    onStatusChange: (StatusFilter) -> Unit,
    projectFilter: Long?,
    onProjectFilterChange: (Long?) -> Unit,
    onToggle: (Task, Boolean) -> Unit,
    onOpen: (Task) -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val projectsById = remember(projects) { projects.associateBy { it.id } }
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search tasks") },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            StatusFilter.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = status == option,
                    onClick = { onStatusChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, StatusFilter.entries.size),
                ) { Text(option.label) }
            }
        }
        if (projects.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = projectFilter == null,
                        onClick = { onProjectFilterChange(null) },
                        label = { Text("All projects") },
                    )
                }
                items(projects, key = { it.id }) { project ->
                    FilterChip(
                        selected = projectFilter == project.id,
                        onClick = { onProjectFilterChange(if (projectFilter == project.id) null else project.id) },
                        label = { Text(project.name, maxLines = 1) },
                        leadingIcon = { ColorDot(projectColor(project.colorIndex)) },
                    )
                }
                item(key = "none") {
                    FilterChip(
                        selected = projectFilter == NO_PROJECT,
                        onClick = { onProjectFilterChange(if (projectFilter == NO_PROJECT) null else NO_PROJECT) },
                        label = { Text("No project") },
                    )
                }
            }
        }

        if (tasks.isEmpty()) {
            val filtered = query.isNotBlank() || projectFilter != null
            EmptyState(
                title = when {
                    filtered -> "No matching tasks"
                    status == StatusFilter.DONE -> "Nothing completed yet"
                    else -> "No tasks yet"
                },
                body = when {
                    filtered -> "Try a different search or filter."
                    status == StatusFilter.DONE -> "Tasks you tick off will be listed here."
                    else -> "Tap New task to add the first thing on your list."
                },
            )
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        project = task.projectId?.let(projectsById::get),
                        today = today,
                        onToggle = { onToggle(task, it) },
                        onClick = { onOpen(task) },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (status != StatusFilter.OPEN && tasks.any { it.done }) {
                    item(key = "clear-completed") {
                        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                            TextButton(onClick = onClearCompleted) { Text("Clear all completed tasks") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectsScreen(
    stats: List<ProjectStats>,
    onOpen: (Project) -> Unit,
    onEdit: (Project) -> Unit,
    onDelete: (Project) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (stats.isEmpty()) {
        EmptyState(
            title = "No projects yet",
            body = "Projects group related tasks, like a client, a release or a part of your job. Tap New project to create one.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(stats, key = { it.project.id }) { item ->
            ProjectCard(item, onOpen, onEdit, onDelete, Modifier.animateItem())
        }
    }
}

@Composable
private fun ProjectCard(
    stats: ProjectStats,
    onOpen: (Project) -> Unit,
    onEdit: (Project) -> Unit,
    onDelete: (Project) -> Unit,
    modifier: Modifier = Modifier,
) {
    val project = stats.project
    val color = projectColor(project.colorIndex)
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth().clickable { onOpen(project) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(color))
            Column(
                modifier = Modifier.weight(1f).padding(start = 14.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        stats.total == 0 -> "No tasks yet"
                        stats.open == 0 -> "All ${stats.done} done"
                        else -> "${stats.open} open · ${stats.done} done"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { stats.progress },
                    color = color,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        painterResource(R.drawable.ic_more_vert),
                        contentDescription = "Options for ${project.name}",
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit(project) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete(project) })
                }
            }
        }
    }
}
