package com.kabab.work

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kabab.work.data.Task
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Drives the app the way a person would and checks what ends up on screen and in the database. */
@RunWith(RobolectricTestRunner::class)
// Tall enough that whole forms fit on screen. The width stays at Robolectric's default 320dp: on wider
// simulated screens a text field inside a dialog window keeps Compose from ever reporting idle.
@Config(qualifiers = "w320dp-h915dp")
class AppUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val app by lazy { AppHarness(compose) }
    private val today get() = app.today

    @After
    fun tearDown() = app.close()

    @Test
    fun `a task added from Today is saved as due today`() {
        app.launch()
        app.waitForText("Nothing scheduled")

        compose.onNodeWithContentDescription("New task").performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Title").performTextInput("Write status update")
        compose.onNodeWithText("Save").performClick()

        app.waitForText("Write status update")
        app.waitForNoText("Nothing scheduled")
        val saved = app.savedTasks().single()
        assertEquals("Write status update", saved.title)
        assertEquals(today.toEpochDay(), saved.dueEpochDay)
        assertEquals(app.noon, saved.createdAt)
    }

    @Test
    fun `ticking a task moves it to Completed today`() {
        app.addTask(Task(title = "File expenses", dueEpochDay = today.toEpochDay()))
        app.launch()
        app.waitForText("File expenses")
        compose.onAllNodesWithText("Completed today").assertCountEquals(0)

        compose.onNodeWithContentDescription("Complete File expenses").performClick()

        app.waitForText("Completed today")
        compose.onNodeWithText("All 1 done for today").assertIsDisplayed()
        val saved = app.savedTasks().single()
        assertEquals(true, saved.done)
        assertEquals(app.noon, saved.completedAt)
    }

    @Test
    fun `editing a task changes its priority and clears its due date`() {
        app.addTask(Task(title = "Plan offsite", dueEpochDay = today.toEpochDay()))
        app.launch()
        app.waitForText("Plan offsite")

        compose.onNodeWithText("Plan offsite").performClick()
        app.waitForText("Edit task")
        compose.onNodeWithText("High").performClick()
        compose.onNodeWithText("Tomorrow").performClick()
        compose.onNodeWithText("Tomorrow").performClick() // tapping the selected date again removes it
        compose.onNodeWithText("Save").performClick()

        app.waitForText("Nothing scheduled") // no due date any more, so it leaves Today
        val saved = app.savedTasks().single()
        assertEquals(2, saved.priority)
        assertNull(saved.dueEpochDay)
    }

    @Test
    fun `a new project can be created and given a task`() {
        app.launch()
        compose.onNodeWithText("Projects").performClick()
        app.waitForText("No projects yet")

        compose.onNodeWithContentDescription("New project").performClick()
        compose.onNodeWithText("Name").performTextInput("Client onboarding")
        compose.onNodeWithContentDescription("Colour 3").performClick()
        compose.onNodeWithText("Save").performClick()
        app.waitForText("No tasks yet")
        val project = app.savedProjects().single()
        assertEquals("Client onboarding", project.name)
        assertEquals(2, project.colorIndex)

        // Opening the project and adding a task from there files the task under it.
        compose.onNodeWithText("Client onboarding").performClick()
        app.waitForText("No matching tasks")
        compose.onNodeWithContentDescription("New task").performClick()
        compose.onNodeWithText("Title").performTextInput("Send welcome pack")
        compose.onNodeWithText("Save").performClick()

        app.waitForText("Send welcome pack")
        assertEquals(project.id, app.savedTasks().single().projectId)
    }

    @Test
    fun `opening a project shows only its open tasks`() {
        app.seed()
        app.launch()
        app.waitForText("Review homepage copy")

        compose.onNodeWithText("Projects").performClick()
        app.waitForText("Q4 planning")
        compose.onNodeWithText("2 open · 1 done").assertIsDisplayed() // Website relaunch
        compose.onNodeWithText("Website relaunch").performClick()

        app.waitForText("Fix checkout bug")
        compose.onNodeWithText("Review homepage copy").assertIsDisplayed()
        compose.onAllNodesWithText("Reply to candidate emails").assertCountEquals(0) // other project
        compose.onAllNodesWithText("Update team wiki").assertCountEquals(0) // done
    }

    @Test
    fun `search narrows the task list`() {
        app.seed()
        app.launch()
        compose.onNodeWithText("Tasks").performClick()
        app.waitForText("Clean up shared drive")

        compose.onNodeWithText("Search tasks").performTextInput("invoice")

        app.waitForNoText("Clean up shared drive")
        compose.onNodeWithText("Send invoice to Acme").assertIsDisplayed()
    }

    @Test
    fun `deleting a task can be undone`() {
        app.addTask(Task(title = "Order laptops", dueEpochDay = today.toEpochDay()))
        app.launch()
        app.waitForText("Order laptops")

        compose.onNodeWithText("Order laptops").performClick()
        compose.onNodeWithText("Delete").performClick()
        app.waitForText("Task deleted")
        app.waitForNoText("Order laptops")

        compose.onNodeWithText("Undo").performClick()
        app.waitForText("Order laptops")
        assertEquals(1, app.savedTasks().size)
    }
}
