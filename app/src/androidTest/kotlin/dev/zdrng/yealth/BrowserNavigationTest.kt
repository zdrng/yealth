package dev.zdrng.yealth

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.core.app.ApplicationProvider
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import dev.zdrng.yealth.domain.model.HealthCategory
import dev.zdrng.yealth.ui.MainActivity
import dev.zdrng.yealth.ui.feature.typeLabels
import org.junit.Rule
import org.junit.Test
import java.io.File

class BrowserNavigationTest {
    @get:Rule val compose = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>(
        ActivityScenarioRule(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).putExtra("preview", true)),
        { rule -> lateinit var activity: MainActivity; rule.scenario.onActivity { activity = it }; activity },
    )
    private fun click(tag: String) = compose.onNodeWithTag(tag).performClick()
    private fun scrollClick(list: String, tag: String) {
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        click(tag)
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(350) // Let platform window/surface transitions settle, too.
        val output = File(File(instrumentation.targetContext.getExternalFilesDir(null), "m3-regression").apply { mkdirs() }, "m2-$name.png")
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("screencap -p")).use { input ->
            output.outputStream().use { input.copyTo(it) }
        }
        check(output.length() > 1000) { "Screenshot capture failed: $name" }
    }
    private fun search(id: String) {
        val label = compose.activity.getString(typeLabels.getValue(id))
        compose.onNodeWithTag("catalog-search").performTextClearance()
        compose.onNodeWithTag("catalog-search").performTextInput(label)
        scrollClick("screen-browse", "type-$id")
    }

    @Test fun todayDetailDatesAndFocus() {
        capture("today")
        scrollClick("screen-today", "summary-steps")
        scrollClick("screen-metric", "record-sample-steps-2026-09-18")
        compose.onNodeWithTag("navigation-bar").assertDoesNotExist()
        compose.onNodeWithTag("screen-record").assertExists()
        capture("steps-detail")
        click("back")
        click("next-date")
        compose.onNodeWithTag("empty-records").assertExists()
        click("previous-date")
        click("choose-date")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("screen-metric").assertExists()
        click("back")
        compose.onNodeWithTag("nav-today").assertIsSelected()
    }

    @Test fun tabStacksSearchAndSelectionSurviveRecreation() {
        click("nav-browse")
        search("vo2_max")
        compose.onNodeWithTag("period-DAYS_30").assertIsSelected()
        click("period-DAYS_90")
        click("nav-access")
        compose.onNodeWithTag("nav-access").assertIsSelected()
        click("nav-browse")
        compose.onNodeWithTag("period-DAYS_90").assertIsSelected()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("period-DAYS_90").assertIsSelected()
        compose.onNodeWithTag("nav-browse").assertIsSelected()
        capture("vo2-history")
        scrollClick("screen-metric", "record-sample-vo2_max-2026-09-09")
        click("back")
        compose.onNodeWithTag("record-sample-vo2_max-2026-09-09").assertIsDisplayed()
        click("back")
        compose.onNodeWithTag("catalog-search").assertTextContains("VO₂ max")
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithTag("screen-browse").assertExists()
    }

    @Test fun allCatalogRoutesAndDetailsAreReachable() {
        click("nav-browse")
        HealthCategory.entries.forEach { category ->
            scrollClick("screen-browse", "category-${category.name}")
            compose.onNodeWithTag("screen-category").assertExists()
            compose.onNodeWithTag("nav-browse").assertIsSelected()
            click("back")
        }
        compose.onNodeWithTag("screen-browse").performScrollToIndex(0)
        typeLabels.keys.forEach { id ->
            search(id)
            compose.onNodeWithTag("screen-metric").assertExists()
            val day = if (id == "nutrition" || id == "hydration") {
                click("previous-date")
                "2026-09-17"
            } else "2026-09-18"
            scrollClick("screen-metric", "record-sample-$id-$day")
            compose.onNodeWithTag("screen-record").assertExists()
            click("back")
            click("back")
        }
    }

    @Test fun mockupScreensAndAccessActions() {
        click("nav-browse")
        capture("browse")
        scrollClick("screen-browse", "category-ACTIVITY")
        capture("activity")
        click("period-WEEK")
        scrollClick("screen-category", "type-steps")
        compose.onNodeWithTag("period-WEEK").assertIsSelected()
        compose.onNodeWithTag("screen-metric").performScrollToNode(hasTestTag("sample-steps-chart"))
        capture("steps-week")
        click("back")
        compose.onNodeWithTag("period-WEEK").assertIsSelected()
        click("back")
        scrollClick("screen-browse", "category-NUTRITION")
        compose.onNodeWithTag("empty-records").assertExists()
        capture("nutrition-empty")
        click("previous-date")
        compose.onNodeWithTag("empty-records").assertDoesNotExist()
        click("back")
        compose.onNodeWithTag("screen-browse").performScrollToIndex(0)
        search("vo2_max")
        capture("vo2-default")
        click("back")
        search("resting_heart_rate")
        capture("resting-heart-rate")
        click("nav-access")
        capture("access")
        HealthCategory.entries.forEach { category ->
            scrollClick("screen-access", "access-${category.name}")
            compose.onNodeWithText("Close").performClick()
        }
        compose.onNodeWithContentDescription("Access & privacy").performClick()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("nav-access").assertIsSelected()
    }

    @Test fun adaptiveLayoutAndDateSelection() {
        val rail = compose.activity.resources.configuration.screenWidthDp >= 600
        compose.onNodeWithTag(if (rail) "navigation-rail" else "navigation-bar").assertExists()
        capture("adaptive-today")
        click("choose-date")
        capture("date-picker")
        compose.onNodeWithText("Thursday, September 17, 2026").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithTag("choose-date").assertContentDescriptionContains("September 17, 2026", substring = true)
        click("nav-browse")
        capture("adaptive-browse")
        scrollClick("screen-browse", "category-NUTRITION")
        capture("adaptive-nutrition")
        click("nav-access")
        compose.onNodeWithTag("nav-access").assertIsSelected()
    }
}
