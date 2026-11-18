package com.kabab.work.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kabab.work.R
import com.kabab.work.data.Project
import com.kabab.work.domain.NO_PROJECT
import com.kabab.work.domain.StatusFilter
import com.kabab.work.domain.bucketForToday
import com.kabab.work.domain.filterTasks
import com.kabab.work.domain.projectStats
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Tab(val label: String, val icon: Int) {
    TODAY("Today", R.drawable.ic_today),
    TASKS("Tasks", R.drawable.ic_list),
    PROJECTS("Projects", R.drawable.ic_folder),
}

/** The current date, refreshed whenever the app comes back to the foreground. */
@Composable
private fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose { }
    }
    return today
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrganizerApp(viewModel: WorkViewModel, today: LocalDate = rememberToday()) {
    val loadedTasks by viewModel.tasks.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val editingProject by viewModel.editingProject.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(StatusFilter.OPEN) }
    var selectedProject by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmClearCompleted by rememberSaveable { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val projectsById = remember(projects) { projects.associateBy { it.id } }
    // Ignore a filter that points at a project which has since been deleted.
    val projectFilter = selectedProject.takeIf { it == NO_PROJECT || it in projectsById }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (tab == Tab.TODAY) "Work Organizer" else tab.label) }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(painterResource(item.icon), contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            val fabLabel = if (tab == Tab.PROJECTS) "New project" else "New task"
            ExtendedFloatingActionButton(
                onClick = {
                    when (tab) {
                        // A task added from Today is due today; one added inside a project filter joins that project.
                        Tab.TODAY -> viewModel.startNewTask(dueEpochDay = today.toEpochDay())
                        Tab.TASKS -> viewModel.startNewTask(projectId = projectFilter?.takeIf { it != NO_PROJECT })
                        Tab.PROJECTS -> viewModel.startNewProject()
                    }
                },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(fabLabel) },
                // The button does not expose its text to accessibility services by itself.
                modifier = Modifier.semantics { contentDescription = fabLabel },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // Stay blank until the first database read finishes, rather than flashing an empty state.
            val tasks = loadedTasks ?: return@Box
            when (tab) {
                Tab.TODAY -> TodayScreen(
                    buckets = remember(tasks, today) { bucketForToday(tasks, today) },
                    projectsById = projectsById,
                    today = today,
                    onToggle = viewModel::setDone,
                    onOpen = viewModel::editTask,
                )

                Tab.TASKS -> TasksScreen(
                    tasks = remember(tasks, query, status, projectFilter) {
                        filterTasks(tasks, query, status, projectFilter)
                    },
                    projects = projects,
                    today = today,
                    query = query,
                    onQueryChange = { query = it },
                    status = status,
                    onStatusChange = { status = it },
                    projectFilter = projectFilter,
                    onProjectFilterChange = { selectedProject = it },
                    onToggle = viewModel::setDone,
                    onOpen = viewModel::editTask,
                    onClearCompleted = { confirmClearCompleted = true },
                )

                Tab.PROJECTS -> ProjectsScreen(
                    stats = remember(projects, tasks) { projectStats(projects, tasks) },
                    onOpen = { project ->
                        selectedProject = project.id
                        query = ""
                        status = StatusFilter.OPEN
                        tab = Tab.TASKS
                    },
                    onEdit = viewModel::editProject,
                    onDelete = { projectToDelete = it },
                )
            }
        }
    }

    editingTask?.let { task ->
        TaskEditorSheet(
            task = task,
            projects = projects,
            today = today,
            onSave = {
                viewModel.saveTask(it)
                viewModel.closeTaskEditor()
            },
            onDelete = {
                viewModel.deleteTask(task)
                viewModel.closeTaskEditor()
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = "Task deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreTask(task)
                }
            },
            onDismiss = viewModel::closeTaskEditor,
        )
    }

    editingProject?.let { project ->
        ProjectEditorDialog(
            project = project,
            onSave = {
                viewModel.saveProject(it)
                viewModel.closeProjectEditor()
            },
            onDismiss = viewModel::closeProjectEditor,
        )
    }

    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete ${project.name}?") },
            text = { Text("Its tasks are kept and moved to No project.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProject(project); projectToDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { projectToDelete = null }) { Text("Cancel") } },
        )
    }

    if (confirmClearCompleted) {
        AlertDialog(
            onDismissRequest = { confirmClearCompleted = false },
            title = { Text("Clear completed tasks?") },
            text = { Text("Every completed task, in all projects, will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCompletedTasks(); confirmClearCompleted = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClearCompleted = false }) { Text("Cancel") } },
        )
    }
}
