package dev.zdrng.yealth

import androidx.compose.ui.test.*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.time.*

/** Opt-in: requires the documented synthetic Toolbox records and Steps read permission. */
class StepsDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val service get() = (context.applicationContext as YealthApplication).container.healthBrowser
    private fun click(tag: String) = compose.onNodeWithTag(tag).performClick()
    private fun scroll(list: String, matcher: SemanticsMatcher) = compose.onNodeWithTag(list).performScrollToNode(matcher)
    private fun evidence(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(300)
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("screencap -p")).use { input ->
            File(context.getExternalFilesDir(null), "m4-$name.png").outputStream().use { input.copyTo(it) }
        }
    }
    @Test fun toolboxSourceComparison() = runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("stepsFixtures") == "true")
        val date = LocalDate.of(2026, 9, 18); val zone = ZoneId.of("Europe/Berlin")
        val range = HealthTimeRange(date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
        val query = RecordQuery("steps", range, pageSize = 1)
        var cursor: RecordCursor? = null
        val records = mutableListOf<HealthRecord>()
        var pages = 0
        do {
            val state = service.records(RecordRequest(query, cursor))
            check(state is RecordsState.Content) { state.toString() }
            records += state.page.records; cursor = state.page.next; pages++
            check(pages < 1000)
        } while (cursor != null)
        val total = service.stepsSummary(query)
        File(context.getExternalFilesDir(null), "m4-source-comparison.txt").writeText("pages=$pages\nrecords=$records\nsummary=$total\n")
        assertEquals(2, records.size)
        assertTrue(pages >= 2)
        assertEquals(records.size, records.map { it.metadata.id }.toSet().size)
        assertTrue(records.all { it.metadata.originPackage == "androidx.health.connect.client.devtool" })
        assertEquals(setOf(120L, 240L), records.map { (it.fields.single { field -> field.key == "count" }.value as HealthValue.Integer).value }.toSet())
        assertTrue(records.all { (it.time as RecordTime.Interval).startOffset == ZoneOffset.ofHours(2) && it.time.endOffset == ZoneOffset.ofHours(2) })
        assertTrue(records.all { Duration.between((it.time as RecordTime.Interval).start, it.time.end).toMinutes() == 30L })
        assertTrue(total is StepsSummaryState.Available)
        assertEquals(360L, (total as StepsSummaryState.Available).total.count)
        assertEquals(setOf("androidx.health.connect.client.devtool"), total.total.origins)
        val days = (6L downTo 0L).map { back ->
            val day = date.minusDays(back)
            HealthTimeRange(day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant())
        }
        val daily = service.dailySteps(query.copy(range = HealthTimeRange(days.first().start, days.last().end)), days) as DailyStepsState.Available
        assertEquals(7, daily.totals.size)
        assertEquals(360L, daily.totals.last().count)
        assertTrue(daily.totals.dropLast(1).all { it.count == null || it.count == 0L })
        File(context.getExternalFilesDir(null), "m5-daily-source-comparison.txt").writeText(daily.toString())
        val atTwenty = date.atTime(20, 0).atZone(zone).toInstant()
        val narrow = service.records(RecordRequest(query.copy(range = HealthTimeRange(atTwenty, atTwenty.plusSeconds(1800)), pageSize = 100))) as RecordsState.Content
        assertEquals(listOf(240L), narrow.page.records.map { (it.fields.single().value as HealthValue.Integer).value })
        assertTrue(service.records(RecordRequest(query.copy(range = HealthTimeRange(range.end, range.end.plusSeconds(86400))))) is RecordsState.Empty)
    }

    @Test fun liveProviderPagingAcrossDetail() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("stepsFixtures") == "true")
        val container = (context.applicationContext as YealthApplication).container
        val repository = object : dev.zdrng.yealth.domain.repository.HealthRepository by container.healthRepository {
            override suspend fun read(request: RecordRequest): PageResult = container.healthRepository.read(
                RecordRequest(request.query.copy(pageSize = 1), request.cursor))
        }
        val paged = dev.zdrng.yealth.domain.service.HealthBrowserService(repository)
        lateinit var access: dev.zdrng.yealth.ui.feature.AccessViewModel
        compose.activityRule.scenario.onActivity { activity ->
            access = dev.zdrng.yealth.ui.feature.AccessViewModel(paged)
            activity.setContent { dev.zdrng.yealth.ui.theme.YealthTheme {
                dev.zdrng.yealth.ui.navigation.YealthApp(container.browserContent.copy(initialDate = LocalDate.of(2026, 9, 18)), access, paged)
            } }
            access.foreground()
        }
        scroll("screen-today", hasTestTag("summary-steps")); click("summary-steps")
        scroll("screen-metric", hasTestTag("load-more")); evidence("page-one")
        click("load-more")
        scroll("screen-metric", hasText("2 records in the queried range"))
        compose.onNodeWithTag("load-more").assertDoesNotExist()
        scroll("screen-metric", hasText("240 steps")); compose.onNodeWithText("240 steps").performClick()
        compose.onNodeWithTag("screen-record").assertExists(); evidence("page-two-detail")
        click("back")
        scroll("screen-metric", hasText("2 records in the queried range"))
        scroll("screen-metric", hasText("240 steps")); evidence("pages-retained")
        compose.runOnIdle { access.background() }
    }

    @Test fun deniedSummariesAndRecords() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("stepsDenied") == "true")
        scroll("screen-today", hasTestTag("summary-steps"))
        compose.waitUntil(10000) { compose.onAllNodesWithText("Allow read access", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("steps-total", useUnmergedTree = true).assertDoesNotExist()
        evidence("revoked-summary")
        click("summary-steps")
        scroll("screen-metric", hasText("Review access"))
        compose.onNodeWithText("No records for this range").assertDoesNotExist()
        compose.onNodeWithText("360 steps").assertDoesNotExist()
        evidence("revoked-records")
    }

    @Test fun todayActivityStepsDetail() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("stepsFixtures") == "true")
        if (LocalDate.now() != LocalDate.of(2026, 9, 18)) {
            click("choose-date")
            compose.onNodeWithText("Friday, September 18, 2026").performClick()
            compose.onNodeWithText("OK").performClick()
        }
        scroll("screen-today", hasTestTag("summary-steps"))
        compose.waitUntil(10000) { compose.onAllNodesWithTag("steps-total", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("steps-total", useUnmergedTree = true).assertTextEquals("360 steps"); evidence("today")
        scroll("screen-today", hasTestTag("open-activity")); click("open-activity")
        scroll("screen-category", hasTestTag("type-steps")); evidence("activity-day")
        scroll("screen-category", hasTestTag("period-WEEK")); click("period-WEEK")
        scroll("screen-category", hasTestTag("type-steps")); evidence("activity-week")
        click("type-steps")
        scroll("screen-metric", hasTestTag("period-WEEK")); compose.onNodeWithTag("period-WEEK").assertIsSelected()
        scroll("screen-metric", hasText("Health Connect applies source priorities", substring = true)); evidence("aggregate")
        scroll("screen-metric", hasText("120 steps"))
        compose.onAllNodesWithText("120 steps").onFirst().performClick()
        compose.onNodeWithTag("screen-record").assertExists(); evidence("detail")
        scroll("screen-record", hasText("androidx.health.connect.client.devtool"))
        compose.onNodeWithText("androidx.health.connect.client.devtool").assertExists()
        scroll("screen-record", hasText("Open Health Connect settings")); evidence("metadata")
        click("back")
        compose.onNodeWithTag("screen-metric").performScrollToIndex(0); click("period-MONTH")
        click("previous-date")
        scroll("screen-metric", hasText("No records for this range")); evidence("empty-older-range")
        scroll("screen-metric", hasTestTag("next-date")); click("next-date")
        scroll("screen-metric", hasText("120 steps"))
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("screen-metric").performScrollToIndex(0); compose.onNodeWithTag("period-MONTH").assertIsSelected()
        scroll("screen-metric", hasText("120 steps")); evidence("restored")
    }
}
