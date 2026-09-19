package dev.zdrng.yealth

import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import dev.zdrng.yealth.ui.MainActivity
import dev.zdrng.yealth.ui.navigation.YealthApp
import dev.zdrng.yealth.ui.theme.YealthTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Labeled synthetic preview, including deterministic fallback color review. */
class DesignDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        val i = InstrumentationRegistry.getInstrumentation()
        val file = File(File(i.targetContext.getExternalFilesDir(null), "m5-design").apply { mkdirs() }, "$name.png")
        ParcelFileDescriptor.AutoCloseInputStream(i.uiAutomation.executeShellCommand("screencap -p")).use { input -> file.outputStream().use { input.copyTo(it) } }
    }
    @Test fun expressiveReferenceScreens() {
        val content = (compose.activity.application as YealthApplication).container.previewContent
        compose.runOnUiThread { compose.activity.setContent { YealthTheme(dynamicColor = false) { YealthApp(content) } } }
        capture("today")
        compose.onNodeWithTag("nav-browse").performClick(); capture("browse")
        compose.onNodeWithTag("screen-browse").performScrollToNode(hasTestTag("category-ACTIVITY"))
        compose.onNodeWithTag("category-ACTIVITY").performClick(); capture("activity")
        compose.onNodeWithTag("period-WEEK").performClick()
        compose.onNodeWithTag("screen-category").performScrollToNode(hasTestTag("type-steps"))
        compose.onNodeWithTag("type-steps").performClick()
        capture("metric-first-view")
        compose.onNodeWithTag("screen-metric").performScrollToNode(hasTestTag("sample-steps-chart"))
        compose.onNodeWithTag("sample-steps-chart").assertIsDisplayed(); capture("week-chart")
        if (compose.activity.resources.configuration.fontScale > 1.3f) {
            compose.onNodeWithTag("screen-metric").performScrollToNode(hasTestTag("chart-value-6"))
            compose.onNodeWithTag("chart-value-6").assertTextContains("6842 steps", substring = true)
            capture("week-chart-values")
        }
        val latest = content.records.filter { it.typeId == "steps" }.maxBy { record ->
            when (val time = record.time) {
                is dev.zdrng.yealth.domain.model.RecordTime.Interval -> time.start
                is dev.zdrng.yealth.domain.model.RecordTime.Point -> time.time
            }
        }
        compose.onNodeWithTag("screen-metric").performScrollToNode(hasTestTag("record-${latest.metadata.id}"))
        compose.onNodeWithTag("record-${latest.metadata.id}").performClick()
        compose.onNodeWithTag("record-metric-value").assertIsDisplayed()
        capture("record-first-view")
    }
}
