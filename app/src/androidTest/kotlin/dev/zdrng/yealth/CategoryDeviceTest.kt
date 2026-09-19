package dev.zdrng.yealth

import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.*
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.test.platform.app.InstrumentationRegistry
import dev.zdrng.yealth.data.healthconnect.*
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.service.HealthBrowserService
import dev.zdrng.yealth.ui.MainActivity
import dev.zdrng.yealth.ui.feature.*
import dev.zdrng.yealth.ui.navigation.YealthApp
import dev.zdrng.yealth.ui.theme.YealthTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Actual production SDK mappers/service/navigation with explicit test-only source records. */
class CategoryDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private class Gateway : HealthConnectGateway {
        val catalog = HealthRecordCatalog()
        val fixtures = StableRecordFixtures.all()
        override fun sdkStatus() = HealthConnectClient.SDK_AVAILABLE
        override fun featureStatus(feature: Int) = HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        override suspend fun grantedPermissions() = catalog.entries.map { it.type.readPermission }.toSet() + "android.permission.health.READ_HEALTH_DATA_HISTORY"
        override suspend fun aggregate(request: AggregateRequest) = AggregationResult(
            mapOf(HeartRateRecord.BPM_AVG.metricKey to 72L, SleepSessionRecord.SLEEP_DURATION_TOTAL.metricKey to 26_640_000L,
                StepsRecord.COUNT_TOTAL.metricKey to 6842L, ExerciseSessionRecord.EXERCISE_DURATION_TOTAL.metricKey to 60_000L),
            mapOf(DistanceRecord.DISTANCE_TOTAL.metricKey to 4800.0), emptySet())
        @Suppress("UNCHECKED_CAST") override suspend fun <T : Record> read(request: ReadRecordsRequest<T>): ReadRecordsResponse<T> {
            val id = catalog.entries.single { it.recordClass == request.recordType }.type.id
            val inRange = request.timeRangeFilter.startTime!! <= StableRecordFixtures.start && request.timeRangeFilter.endTime!! > StableRecordFixtures.start
            return ReadRecordsResponse(if (inRange) listOf(fixtures.getValue(id) as T) else emptyList(), null)
        }
    }
    private fun click(tag: String) = compose.onNodeWithTag(tag).performClick()
    private fun scroll(list: String, tag: String) { compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag)) }
    private fun capture(name: String) {
        compose.waitForIdle()
        val i = InstrumentationRegistry.getInstrumentation()
        val file = File(File(i.targetContext.getExternalFilesDir(null), "m5").apply { mkdirs() }, "$name.png")
        ParcelFileDescriptor.AutoCloseInputStream(i.uiAutomation.executeShellCommand("screencap -p")).use { input -> file.outputStream().use { input.copyTo(it) } }
    }
    private fun setup(): kotlinx.coroutines.CoroutineScope {
        val scope = MainScope(); val repo = HealthConnectRepository(Gateway()); val service = HealthBrowserService(repo)
        compose.runOnUiThread {
            val access = AccessViewModel(service, scope); access.foreground()
            compose.activity.setContent { YealthTheme {
                YealthApp(BrowserContent(repo.recordTypes, emptyList(), isPreview = false, initialDate = LocalDate.parse("2026-09-18")), access, service)
            } }
        }
        return scope
    }
    @Test fun everyStableAdapterHasVisibleRawDetailAndNestedData() {
        val scope = setup()
        try {
            click("nav-browse")
            for (id in StableRecordFixtures.all().keys) {
                val label = compose.activity.getString(typeLabels.getValue(id))
                compose.onNodeWithTag("catalog-search").performTextClearance()
                compose.onNodeWithTag("catalog-search").performTextInput(label)
                scroll("screen-browse", "type-$id"); click("type-$id")
                compose.waitUntil(10_000) { compose.onAllNodesWithTag("records-loading").fetchSemanticsNodes().isEmpty() }
                scroll("screen-metric", "record-"); click("record-")
                compose.onNodeWithTag("screen-record").assertExists()
                if (id in setOf("sleep_session", "exercise_session", "nutrition", "planned_exercise_session", "skin_temperature")) capture("detail-$id")
                click("back"); click("back")
            }
        } finally { scope.cancel() }
    }
    @Test fun liveSummariesWeeklyChartAndDateSpecificNutritionEmpty() {
        val scope = setup()
        try {
            scroll("screen-today", "summary-heart_rate")
            compose.waitUntil(10_000) { compose.onAllNodesWithText("72 bpm", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("screen-today").performScrollToIndex(0)
            capture("today-fixtures")
            scroll("screen-today", "summary-heart_rate"); click("summary-heart_rate")
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("metric-summary-value").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("metric-summary-value").assertTextEquals("72 bpm")
            capture("heart-metric"); click("back")
            scroll("screen-today", "summary-sleep_session"); click("summary-sleep_session")
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("metric-summary-value").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("metric-summary-value").assertTextEquals("444 min")
            capture("sleep-metric"); click("back")
            scroll("screen-today", "summary-steps"); click("summary-steps"); click("period-WEEK")
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("daily-steps-chart").fetchSemanticsNodes().isNotEmpty() }
            scroll("screen-metric", "daily-steps-chart"); capture("weekly-chart-fixtures")
            click("nav-browse"); scroll("screen-browse", "category-NUTRITION"); click("category-NUTRITION")
            compose.waitUntil(10_000) { compose.onAllNodesWithText("0.75 L", substring = true).fetchSemanticsNodes().isNotEmpty() }
            click("previous-date")
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("empty-records").fetchSemanticsNodes().isNotEmpty() }
            capture("nutrition-empty-live")
            compose.onNodeWithTag("choose-date").assertExists()
        } finally { scope.cancel() }
    }
    @Test fun routeConsentIsSeparateAndCancellationKeepsRawSession() {
        val scope = MainScope()
        val base = HealthConnectRepository(Gateway())
        val repo = object : dev.zdrng.yealth.domain.repository.HealthRepository by base {
            override suspend fun read(request: RecordRequest): PageResult = when (val result = base.read(request)) {
                is PageResult.Success -> result.copy(page = result.page.copy(records = result.page.records.map { record ->
                    if (record.typeId != "exercise_session") record else record.copy(fields = record.fields.map {
                        if (it.key == "exerciseRouteResult") RecordField(it.key, HealthValue.Fields(listOf(RecordField("status", HealthValue.Text("consent_required"))))) else it
                    })
                }))
                else -> result
            }
        }
        val service = HealthBrowserService(repo)
        val route = androidx.compose.runtime.mutableStateOf(RouteUiState())
        compose.runOnUiThread {
            val access = AccessViewModel(service, scope); access.foreground()
            compose.activity.setContent { YealthTheme {
                YealthApp(BrowserContent(repo.recordTypes, initialDate = LocalDate.parse("2026-09-18")), access, service,
                    routeState = route.value, requestRoute = { id -> route.value = RouteUiState(id, cancelled = true) })
            } }
        }
        try {
            click("nav-browse")
            compose.onNodeWithTag("catalog-search").performTextInput("Exercise session")
            scroll("screen-browse", "type-exercise_session"); click("type-exercise_session")
            scroll("screen-metric", "record-"); click("record-")
            scroll("screen-record", "request-route"); capture("route-consent")
            click("request-route")
            compose.onNodeWithText("Route was not shared. You can try again.").assertExists()
            compose.onNodeWithTag("screen-record").assertExists()
            compose.onNodeWithText("No records for this range").assertDoesNotExist()
            capture("route-cancelled")
        } finally { scope.cancel() }
    }

}
