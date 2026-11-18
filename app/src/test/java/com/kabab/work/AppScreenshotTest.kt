package com.kabab.work

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders each screen with sample data at a phone-sized resolution.
 *
 * `./gradlew :app:testDebugUnitTest -Pscreenshots` writes the PNGs to `app/build/screenshots`;
 * without the flag this only checks that every screen draws without crashing.
 *
 * The project dialog is not captured: at this screen width a text field inside a dialog window keeps
 * Compose from ever reporting idle under Robolectric. AppUiTest covers that dialog instead.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-420dpi")
class AppScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val app by lazy { AppHarness(compose) }

    @After
    fun tearDown() = app.close()

    @Test
    fun `screens render`() {
        app.seed()
        app.launch()
        app.waitForText("Review homepage copy")
        captureScreenRoboImage("build/screenshots/1-today.png")

        compose.onNodeWithText("Review homepage copy").performClick()
        app.waitForText("Edit task")
        captureScreenRoboImage("build/screenshots/2-edit-task.png")
        compose.onNodeWithText("Cancel").performClick()
        app.waitForNoText("Edit task")

        compose.onNodeWithText("Tasks").performClick()
        app.waitForText("Clean up shared drive")
        captureScreenRoboImage("build/screenshots/3-tasks.png")

        compose.onNodeWithText("Projects").performClick()
        app.waitForText("Q4 planning")
        captureScreenRoboImage("build/screenshots/4-projects.png")
    }

    @Test
    fun `today renders in dark theme`() {
        app.seed()
        app.launch(darkTheme = true)
        app.waitForText("Review homepage copy")
        captureScreenRoboImage("build/screenshots/5-today-dark.png")
    }
}
