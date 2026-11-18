package com.kabab.work.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kabab.work.R
import com.kabab.work.data.Priority
import com.kabab.work.data.Project
import com.kabab.work.data.Task
import com.kabab.work.domain.dueLabel
import com.kabab.work.domain.isOverdue
import com.kabab.work.ui.theme.projectColor
import java.time.LocalDate

@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
fun SectionHeader(title: String, count: Int, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.SemiBold)
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    project: Project?,
    today: LocalDate,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overdue = isOverdue(task, today)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.done,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics {
                contentDescription = if (task.done) "Reopen ${task.title}" else "Complete ${task.title}"
            },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val due = task.dueEpochDay?.let(LocalDate::ofEpochDay)
            if (due != null || project != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (due != null) {
                        Text(
                            text = dueLabel(due, today),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (overdue) FontWeight.SemiBold else null,
                        )
                    }
                    if (project != null) {
                        ColorDot(projectColor(project.colorIndex), Modifier.padding(start = if (due != null) 4.dp else 0.dp))
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (!task.done && task.priority == Priority.HIGH.level) {
            Icon(
                painter = painterResource(R.drawable.ic_flag),
                contentDescription = "High priority",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp).size(20.dp),
            )
        }
    }
}
