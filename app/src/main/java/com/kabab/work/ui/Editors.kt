package com.kabab.work.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kabab.work.R
import com.kabab.work.data.Priority
import com.kabab.work.data.Project
import com.kabab.work.data.Task
import com.kabab.work.domain.dueLabel
import com.kabab.work.ui.theme.ProjectPalette
import com.kabab.work.ui.theme.projectColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    task: Task,
    projects: List<Project>,
    today: LocalDate,
    onSave: (Task) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = task.id == 0L
    var title by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var notes by rememberSaveable(task.id) { mutableStateOf(task.notes) }
    var priority by rememberSaveable(task.id) { mutableIntStateOf(task.priority) }
    var dueEpochDay by rememberSaveable(task.id) { mutableStateOf(task.dueEpochDay) }
    var projectId by rememberSaveable(task.id) { mutableStateOf(task.projectId) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (isNew) "New task" else "Edit task", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 2,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            FieldLabel("Due")
            val quickDates = listOf(
                "Today" to today,
                "Tomorrow" to today.plusDays(1),
                "Next Monday" to today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)),
            ).distinctBy { it.second }
            val due = dueEpochDay?.let(LocalDate::ofEpochDay)
            val isCustomDate = due != null && quickDates.none { it.second == due }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickDates.forEach { (label, date) ->
                    FilterChip(
                        selected = due == date,
                        // Tapping the selected chip again removes the due date.
                        onClick = { dueEpochDay = if (due == date) null else date.toEpochDay() },
                        label = { Text(label) },
                    )
                }
                FilterChip(
                    selected = isCustomDate,
                    onClick = { showDatePicker = true },
                    label = { Text(if (isCustomDate) dueLabel(due!!, today) else "Pick date") },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_event), contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }

            FieldLabel("Priority")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Priority.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = priority == option.level,
                        onClick = { priority = option.level },
                        shape = SegmentedButtonDefaults.itemShape(index, Priority.entries.size),
                    ) { Text(option.label) }
                }
            }

            FieldLabel("Project")
            if (projects.isEmpty()) {
                Text(
                    "Create projects in the Projects tab to group related tasks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = projectId == null, onClick = { projectId = null }, label = { Text("None") })
                    projects.forEach { project ->
                        FilterChip(
                            selected = projectId == project.id,
                            onClick = { projectId = project.id },
                            label = { Text(project.name, maxLines = 1) },
                            leadingIcon = { ColorDot(projectColor(project.colorIndex)) },
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(
                            task.copy(
                                title = title,
                                notes = notes,
                                priority = priority,
                                dueEpochDay = dueEpochDay,
                                projectId = projectId,
                            ),
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("Save") }
            }
        }
    }

    if (showDatePicker) {
        // The picker works in UTC midnights, which map 1:1 onto epoch days.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (dueEpochDay ?: today.toEpochDay()) * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueEpochDay = pickerState.selectedDateMillis?.let { Math.floorDiv(it, MILLIS_PER_DAY) }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { dueEpochDay = null; showDatePicker = false }) { Text("No date") }
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectEditorDialog(
    project: Project,
    onSave: (Project) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(project.id) { mutableStateOf(project.name) }
    var colorIndex by rememberSaveable(project.id) { mutableIntStateOf(project.colorIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (project.id == 0L) "New project" else "Edit project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProjectPalette.forEachIndexed { index, color ->
                        val selected = index == colorIndex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .then(
                                    if (selected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(if (selected) 6.dp else 0.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { colorIndex = index }
                                .semantics { contentDescription = "Colour ${index + 1}" },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(project.copy(name = name, colorIndex = colorIndex)) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
