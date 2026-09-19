package dev.zdrng.yealth

import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.repository.HealthRepository
import dev.zdrng.yealth.domain.service.HealthBrowserService
import dev.zdrng.yealth.ui.feature.*
import dev.zdrng.yealth.ui.feature.Period
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class OverviewServiceTest {
    private class Repository : HealthRepository {
        override val recordTypes = listOf("steps", "sleep_session", "heart_rate", "distance", "weight").map {
            RecordType(it, HealthCategory.ACTIVITY, TimeShape.INTERVAL, "read.$it", emptyList(), readerStatus = ReaderStatus.IMPLEMENTED)
        }
        var env = HealthEnvironment(ProviderStatus.AVAILABLE, setOf(HealthFeature.HISTORY), recordTypes.map { it.readPermission }.toSet(), true)
        var summaries = mutableListOf<String>()
        var bins = mutableListOf<HealthTimeRange>()
        var pages = mutableListOf<RecordRequest>()
        var summary: suspend (String, HealthTimeRange) -> MetricSummaryResult = { id, range -> MetricSummaryResult.Success(MetricSummary(id, range, null, setOf("source.one", "source.two"))) }
        override suspend fun environment() = EnvironmentResult.Ready(env)
        override suspend fun metricSummary(typeId: String, range: HealthTimeRange): MetricSummaryResult { summaries += typeId; return summary(typeId, range) }
        override suspend fun stepsTotal(range: HealthTimeRange): StepsTotalResult { bins += range; return StepsTotalResult.Success(StepsTotal(range, if (bins.size == 1) null else 0, emptySet())) }
        override suspend fun read(request: RecordRequest): PageResult { pages += request; return PageResult.Success(RecordPage(emptyList(), null)) }
    }
    private val clock = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC)
    private val zone = ZoneId.of("Europe/Berlin")
    private val selection = LiveSelection("steps", LocalDate.parse("2026-10-25"), Period.WEEK)

    @Test fun `provider summaries preserve null mixed sources and permission failures`() = runBlocking {
        val repo = Repository(); val service = HealthBrowserService(repo)
        val query = selectedRange(selection.copy(type = "sleep_session"), true, clock, zone).query
        val result = service.metricSummary(query) as MetricSummaryState.Available
        assertNull(result.summary.value); assertEquals(setOf("source.one", "source.two"), result.summary.origins)
        repo.env = repo.env.copy(grantedPermissions = emptySet())
        assertTrue((service.metricSummary(query) as MetricSummaryState.Unavailable).reason is RecordsState.AccessRequired)
        assertEquals(listOf("sleep_session"), repo.summaries)
        assertTrue(repo.pages.isEmpty())
    }

    @Test fun `weekly chart uses seven contiguous calendar bins and retains missing versus zero`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = Repository(); val service = HealthBrowserService(repo); val access = AccessViewModel(service, scope)
            val vm = StepsSummaryViewModel(service, access.uiState, clock, zone, scope, includeDaily = true)
            vm.select(selection); vm.attach(); access.foreground()
            val result = withTimeout(3000) { vm.uiState.first { it.dailyTotals.size == 7 } }
            assertEquals(7, result.dailyTotals.size)
            assertEquals(25L, Duration.between(result.dailyTotals.last().range.start, result.dailyTotals.last().range.end).toHours())
            assertEquals(result.dailyTotals.first().range.start, repo.bins.first().start)
            assertTrue(result.dailyTotals.zipWithNext().all { (a,b) -> a.range.end == b.range.start })
            // First aggregate is overall range; each day is still the exact provider response.
            assertTrue(result.dailyTotals.all { it.count == 0L })
            access.background(); assertTrue(vm.uiState.value.dailyTotals.isEmpty())
        } finally { scope.cancel() }
    }

    @Test fun `daily service rejects out of range requests and does not fill gaps with zero`() = runBlocking {
        val repo = Repository(); val service = HealthBrowserService(repo)
        val query = selectedRange(selection, true, clock, zone).query
        val first = HealthTimeRange(query.range.start, query.range.start.plusSeconds(3600))
        assertTrue(service.dailySteps(query, listOf(HealthTimeRange(first.start.minusSeconds(1), first.end))) is DailyStepsState.Unavailable)
        assertTrue(repo.bins.isEmpty())
        val result = service.dailySteps(query, listOf(first)) as DailyStepsState.Available
        assertNull(result.totals.single().count)
    }

    @Test fun `overview reads only visible selected types and clears after revocation or selection`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = Repository(); val service = HealthBrowserService(repo); val access = AccessViewModel(service, scope)
            val vm = OverviewViewModel(service, access.uiState, clock, zone, scope)
            val selected = OverviewSelection(listOf("sleep_session", "heart_rate", "weight"), selection.date, Period.DAY)
            vm.select(selected); access.foreground(); assertTrue(repo.summaries.isEmpty()); vm.attach()
            withTimeout(3000) { vm.uiState.first { it.records["weight"] is RecordsState.Empty } }
            assertEquals(listOf("sleep_session", "heart_rate"), repo.summaries)
            assertEquals(1, repo.pages.single().query.pageSize)
            repo.env = repo.env.copy(grantedPermissions = emptySet()); access.refresh()
            assertTrue((vm.uiState.value.summaries["sleep_session"] as MetricSummaryState.Unavailable).reason is RecordsState.AccessRequired)
            vm.detach(); assertTrue(vm.uiState.value.summaries.isEmpty())
        } finally { scope.cancel() }
    }

    @Test fun `overview cancels in flight old date before publishing newer selection`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = Repository(); val started = CompletableDeferred<Unit>(); val cancelled = CompletableDeferred<Unit>()
            repo.summary = { _, _ -> started.complete(Unit); try { awaitCancellation() } finally { cancelled.complete(Unit) } }
            val service = HealthBrowserService(repo); val access = AccessViewModel(service, scope)
            val vm = OverviewViewModel(service, access.uiState, clock, zone, scope)
            val first = OverviewSelection(listOf("sleep_session"), selection.date, Period.DAY)
            vm.select(first); vm.attach(); access.foreground(); started.await()
            repo.summary = { id, range -> MetricSummaryResult.Success(MetricSummary(id, range, HealthValue.Integer(8, "min"), emptySet())) }
            vm.select(first.copy(date = first.date.minusDays(1))); cancelled.await()
            val result = withTimeout(3000) { vm.uiState.first { it.summaries["sleep_session"] is MetricSummaryState.Available } }
            assertEquals(first.date.minusDays(1), result.selection!!.date)
        } finally { scope.cancel() }
    }
}
