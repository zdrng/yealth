package dev.zdrng.yealth

import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.repository.HealthRepository
import dev.zdrng.yealth.domain.service.HealthBrowserService
import dev.zdrng.yealth.ui.feature.*
import dev.zdrng.yealth.ui.feature.Period
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class StepsSliceTest {
    private val now = Instant.parse("2026-09-18T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val selection = LiveSelection("steps", LocalDate.of(2026, 9, 18), Period.DAY)
    private val type = RecordType("steps", HealthCategory.ACTIVITY, TimeShape.INTERVAL, "read.steps", emptyList(), readerStatus = ReaderStatus.IMPLEMENTED)
    private class Repository(private val type: RecordType) : HealthRepository {
        override val recordTypes = listOf(type)
        var environment = HealthEnvironment(ProviderStatus.AVAILABLE, setOf(HealthFeature.HISTORY), setOf("read.steps"), true)
        var aggregates = mutableListOf<HealthTimeRange>()
        var pages = mutableListOf<RecordRequest>()
        var aggregate: suspend (HealthTimeRange) -> StepsTotalResult = { StepsTotalResult.Success(StepsTotal(it, 6842, setOf("one", "two"))) }
        var reader: suspend (RecordRequest) -> PageResult = { PageResult.Success(RecordPage(emptyList(), null)) }
        override suspend fun environment() = EnvironmentResult.Ready(environment)
        override suspend fun stepsTotal(range: HealthTimeRange): StepsTotalResult { aggregates += range; return aggregate(range) }
        override suspend fun read(request: RecordRequest): PageResult { pages += request; return reader(request) }
    }
    private suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean) = withTimeout(3000) { first(predicate) }
    private fun record(id: String) = HealthRecord("steps", RecordTime.Point(now, null), RecordMetadata(id, "source", now, null, 0, 0, null), listOf(RecordField("count", HealthValue.Integer(100, "count"))))

    @Test fun `local boundaries include 23 and 25 hour days and rolling month`() {
        val zone = ZoneId.of("Europe/Berlin")
        for ((date, hours) in listOf("2026-03-29" to 23L, "2026-10-25" to 25L)) {
            val range = selectedRange(selection.copy(date = LocalDate.parse(date)), true, clock, zone).query.range
            assertEquals(hours, Duration.between(range.start, range.end).toHours())
            assertEquals(LocalDate.parse(date), range.start.atZone(zone).toLocalDate())
            assertEquals(LocalDate.parse(date).plusDays(1), range.end.atZone(zone).toLocalDate())
        }
        val month = selectedRange(selection.copy(date = LocalDate.of(2024, 3, 31), period = Period.MONTH), true, clock, zone).query.range
        assertEquals(LocalDate.of(2024, 3, 1), month.start.atZone(zone).toLocalDate())
        assertEquals(LocalDate.of(2024, 4, 1), month.end.atZone(zone).toLocalDate())
        val week = selectedRange(selection.copy(period = Period.WEEK), true, clock, zone).query.range
        assertEquals(LocalDate.of(2026, 9, 12), week.start.atZone(zone).toLocalDate())
    }
    @Test fun `zone changes resolve the same local selection into new instants`() {
        val utc = selectedRange(selection, true, clock, ZoneOffset.UTC).query.range
        val berlin = selectedRange(selection, true, clock, ZoneId.of("Europe/Berlin")).query.range
        assertEquals(7200, Duration.between(berlin.start, utc.start).seconds)
        val clipped = selectedRange(selection.copy(period = Period.DAYS_90), false, clock, ZoneOffset.UTC)
        assertEquals(now.minus(Duration.ofDays(30)), clipped.recentWindow!!.start)
        assertFalse(clipped.query.requireFullHistory)
    }
    @Test fun `summaries are provider totals and never sums of raw pages`() = runBlocking {
        val repo = Repository(type)
        val query = selectedRange(selection, true, clock, ZoneOffset.UTC).query
        assertEquals(StepsSummaryState.Available(StepsTotal(query.range, 6842, setOf("one", "two"))), HealthBrowserService(repo).stepsSummary(query))
        assertTrue(repo.pages.isEmpty())
        assertEquals(listOf(query.range), repo.aggregates)
    }
    @Test fun `null aggregate remains absent while recorded zero remains zero`() = runBlocking {
        val repo = Repository(type); val service = HealthBrowserService(repo)
        val query = selectedRange(selection, true, clock, ZoneOffset.UTC).query
        for (count in listOf(null, 0L)) {
            repo.aggregate = { StepsTotalResult.Success(StepsTotal(it, count, emptySet())) }
            assertEquals(count, (service.stepsSummary(query) as StepsSummaryState.Available).total.count)
        }
    }
    @Test fun `aggregate gates never convert unavailable or denied access into zero`() = runBlocking {
        val repo = Repository(type); val service = HealthBrowserService(repo)
        val query = selectedRange(selection, true, clock, ZoneOffset.UTC).query
        val original = repo.environment
        for (env in listOf(original.copy(provider = ProviderStatus.UNAVAILABLE), original.copy(grantedPermissions = emptySet()),
            original.copy(historyGranted = false), original.copy(historyGranted = false, features = emptySet()))) {
            repo.environment = env
            assertTrue(service.stepsSummary(query) is StepsSummaryState.Unavailable)
        }
        assertTrue(service.stepsSummary(query.copy(typeId = "heart_rate")) is StepsSummaryState.Unavailable)
        assertTrue(repo.aggregates.isEmpty())
    }
    @Test fun `summary selection cancels old response and clears on revocation`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = Repository(type); val service = HealthBrowserService(repo); val access = AccessViewModel(service, scope)
            val started = CompletableDeferred<Unit>(); val cancelled = CompletableDeferred<Unit>()
            repo.aggregate = { range -> if (range.start == selection.date.atStartOfDay(ZoneOffset.UTC).toInstant()) {
                started.complete(Unit); try { awaitCancellation() } finally { cancelled.complete(Unit) }
            } else StepsTotalResult.Success(StepsTotal(range, 123, emptySet())) }
            val vm = StepsSummaryViewModel(service, access.uiState, clock, ZoneOffset.UTC, scope)
            vm.attach(); vm.select(selection); access.foreground(); withTimeout(3000) { started.await() }
            vm.select(selection.copy(date = selection.date.minusDays(1))); withTimeout(3000) { cancelled.await() }
            assertEquals(123L, (vm.uiState.await { it.state is StepsSummaryState.Available }.state as StepsSummaryState.Available).total.count)
            access.background(); vm.uiState.await { it.state is StepsSummaryState.Loading }
            repo.environment = repo.environment.copy(grantedPermissions = emptySet()); access.foreground()
            assertTrue((vm.uiState.await { it.state is StepsSummaryState.Unavailable }.state as StepsSummaryState.Unavailable).reason is RecordsState.AccessRequired)
        } finally { scope.cancel() }
    }
    @Test fun `hidden summaries do not read and retry uses fresh aggregate`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = Repository(type); val service = HealthBrowserService(repo); val access = AccessViewModel(service, scope)
            val vm = StepsSummaryViewModel(service, access.uiState, clock, ZoneOffset.UTC, scope)
            vm.attach(); vm.select(selection); access.foreground(); vm.uiState.await { it.state is StepsSummaryState.Available }
            repo.aggregate = { StepsTotalResult.Failed(ReadFailure.PROVIDER_ERROR) }; vm.retry()
            vm.uiState.await { it.state is StepsSummaryState.Unavailable }
            repo.aggregate = { StepsTotalResult.Success(StepsTotal(it, 12, emptySet())) }; vm.retry()
            assertEquals(12L, (vm.uiState.await { it.state is StepsSummaryState.Available }.state as StepsSummaryState.Available).total.count)
            vm.detach(); vm.uiState.await { it.state is StepsSummaryState.Loading }; val count = repo.aggregates.size
            access.background(); access.foreground(); yield(); assertEquals(count, repo.aggregates.size)
        } finally { scope.cancel() }
    }
    @Test fun `paging crosses empty intermediate pages without losing or repeating IDs`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = Repository(type); val service = HealthBrowserService(repo); val access = AccessViewModel(service, scope)
            repo.reader = { req -> PageResult.Success(when (req.cursor?.token) {
                null -> RecordPage(listOf(record("a")), RecordCursor(req.query, "empty"))
                "empty" -> RecordPage(emptyList(), RecordCursor(req.query, "last"))
                else -> RecordPage(listOf(record("a"), record("b")), null)
            }) }
            val vm = LiveRecordsViewModel(service, access.uiState, clock, ZoneOffset.UTC, scope)
            vm.attach(); vm.select(selection); access.foreground(); vm.uiState.await { it.records.size == 1 }
            vm.loadMore(); vm.uiState.await { (it.state as? RecordsState.Content)?.page?.next?.token == "last" }
            vm.loadMore(); assertEquals(listOf("a", "b"), vm.uiState.await { it.records.size == 2 }.records.map { it.metadata.id })
            vm.loadMore(); assertEquals(3, repo.pages.size)
        } finally { scope.cancel() }
    }
    @Test fun `detail retains loaded pages while explicit retry still refreshes an unchanged query`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = Repository(type); val service = HealthBrowserService(repo); val access = AccessViewModel(service, scope)
            repo.reader = { request -> PageResult.Success(if (request.cursor == null)
                RecordPage(listOf(record("a")), RecordCursor(request.query, "next")) else RecordPage(listOf(record("b")), null)) }
            val vm = LiveRecordsViewModel(service, access.uiState, clock, ZoneOffset.UTC, scope)
            vm.attach(); vm.select(selection); access.foreground(); vm.uiState.await { it.records.size == 1 }
            vm.loadMore(); vm.uiState.await { it.records.size == 2 }
            vm.attach(); yield(); vm.detach(); yield()
            assertEquals(2, repo.pages.size)
            assertEquals(listOf("a", "b"), vm.uiState.value.records.map { it.metadata.id })
            repo.reader = { PageResult.Success(RecordPage(listOf(record("updated")), null)) }
            vm.retry()
            vm.uiState.await { it.records.singleOrNull()?.metadata?.id == "updated" }
            assertEquals(3, repo.pages.size)
        } finally { scope.cancel() }
    }

    @Test fun `changing range cancels pending page and discards its cursor and rows`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = Repository(type); val service = HealthBrowserService(repo); val access = AccessViewModel(service, scope)
            val started = CompletableDeferred<Unit>(); val cancelled = CompletableDeferred<Unit>()
            repo.reader = { req ->
                if (req.cursor != null) { started.complete(Unit); try { awaitCancellation() } finally { cancelled.complete(Unit) } }
                PageResult.Success(RecordPage(listOf(record(req.query.range.start.toString())), RecordCursor(req.query, "next")))
            }
            val vm = LiveRecordsViewModel(service, access.uiState, clock, ZoneOffset.UTC, scope)
            vm.attach(); vm.select(selection); access.foreground(); vm.uiState.await { it.records.isNotEmpty() }
            vm.loadMore(); withTimeout(3000) { started.await() }
            val next = selection.copy(date = selection.date.minusDays(1)); vm.select(next)
            withTimeout(3000) { cancelled.await() }
            val value = vm.uiState.await { it.selection == next && it.records.isNotEmpty() }
            assertEquals(listOf("2026-09-17T00:00:00Z"), value.records.map { it.metadata.id })
            assertEquals(next.date.atStartOfDay(ZoneOffset.UTC).toInstant(), (value.state as RecordsState.Content).page.next!!.query.range.start)
        } finally { scope.cancel() }
    }
}
