package com.kabab.work.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kabab.work.WorkApp
import com.kabab.work.data.Project
import com.kabab.work.data.Task
import com.kabab.work.data.WorkDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkViewModel(
    private val dao: WorkDao,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** null until the first load finishes, so the UI can tell "loading" from "no tasks". */
    val tasks: StateFlow<List<Task>?> =
        dao.observeTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val projects: StateFlow<List<Project>> =
        dao.observeProjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // The open editors live here so they survive a screen rotation.
    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()

    private val _editingProject = MutableStateFlow<Project?>(null)
    val editingProject: StateFlow<Project?> = _editingProject.asStateFlow()

    fun startNewTask(projectId: Long? = null, dueEpochDay: Long? = null) {
        _editingTask.value = Task(title = "", projectId = projectId, dueEpochDay = dueEpochDay)
    }

    fun editTask(task: Task) {
        _editingTask.value = task
    }

    fun closeTaskEditor() {
        _editingTask.value = null
    }

    fun startNewProject() {
        _editingProject.value = Project(name = "")
    }

    fun editProject(project: Project) {
        _editingProject.value = project
    }

    fun closeProjectEditor() {
        _editingProject.value = null
    }

    fun saveTask(task: Task) {
        val cleaned = task.copy(title = task.title.trim(), notes = task.notes.trim())
        if (cleaned.title.isEmpty()) return
        viewModelScope.launch {
            dao.upsertTask(if (cleaned.id == 0L) cleaned.copy(createdAt = now()) else cleaned)
        }
    }

    fun setDone(task: Task, done: Boolean) {
        viewModelScope.launch {
            dao.upsertTask(task.copy(done = done, completedAt = if (done) now() else null))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.deleteTask(task) }
    }

    /** Puts back a task removed with [deleteTask] (same id, so it lands where it was). */
    fun restoreTask(task: Task) {
        viewModelScope.launch { dao.upsertTask(task) }
    }

    fun deleteCompletedTasks() {
        viewModelScope.launch { dao.deleteCompletedTasks() }
    }

    fun saveProject(project: Project) {
        val cleaned = project.copy(name = project.name.trim())
        if (cleaned.name.isEmpty()) return
        viewModelScope.launch {
            dao.upsertProject(if (cleaned.id == 0L) cleaned.copy(createdAt = now()) else cleaned)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch { dao.deleteProject(project) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WorkApp
                WorkViewModel(app.database.workDao())
            }
        }
    }
}
