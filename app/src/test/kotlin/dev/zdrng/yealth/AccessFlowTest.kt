package dev.zdrng.yealth

import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.repository.HealthRepository
import dev.zdrng.yealth.domain.service.HealthBrowserService
import dev.zdrng.yealth.ui.feature.*
import java.time.*
import dev.zdrng.yealth.ui.feature.Period
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class AccessFlowTest {
    private val steps = RecordType("steps", HealthCategory.ACTIVITY, TimeShape.INTERVAL, "read.steps", emptyList(), readerStatus = ReaderStatus.IMPLEMENTED)
    private val vo2 = steps.copy(id = "vo2_max", readPermission = "read.vo2")
    private val now = Instant.parse("2026-09-18T12:00:00Z")
    private val record = HealthRecord("steps", RecordTime.Point(now, ZoneOffset.UTC), RecordMetadata("id", "source", now, null, 0, 0, null), emptyList())
    private inner class Repository : HealthRepository {
        override val recordTypes = listOf(steps, vo2, steps.copy(id = "planned", readPermission = "read.planned", readerStatus = ReaderStatus.PLANNED), steps.copy(id = "shared_but_planned", readerStatus = ReaderStatus.PLANNED))
        var env = HealthEnvironment(ProviderStatus.AVAILABLE, setOf(HealthFeature.HISTORY), setOf("read.steps"))
        var failure: ReadFailure? = null
        var requests = mutableListOf<RecordRequest>()
        var reader: suspend (RecordRequest) -> PageResult = { PageResult.Success(RecordPage(listOf(record), null)) }
        override suspend fun stepsTotal(range: HealthTimeRange): StepsTotalResult = StepsTotalResult.Failed(ReadFailure.INVALID_REQUEST)
        override suspend fun environment(): EnvironmentResult = failure?.let { EnvironmentResult.Failed(it) } ?: EnvironmentResult.Ready(env)
        override suspend fun read(request: RecordRequest): PageResult { requests += request; return reader(request) }
    }
    private suspend fun <T> StateFlow<T>.await(test: (T) -> Boolean): T = withTimeout(3000) { first(test) }
    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private fun live(repo: Repository, access: AccessViewModel, scope: CoroutineScope, period: Period = Period.DAY): LiveRecordsViewModel =
        LiveRecordsViewModel(HealthBrowserService(repo), access.uiState, Clock.fixed(now, ZoneOffset.UTC), ZoneOffset.UTC, scope).also {
            it.attach()
            it.select(LiveSelection("steps", LocalDate.of(2026, 9, 18), period))
        }

    @Test fun `category requests exclude planned types and reflect partial grants`() {
        val scope = scope()
        try {
            val repo = Repository(); val access = AccessViewModel(HealthBrowserService(repo), scope)
            access.foreground()
            assertEquals(AccessLevel.PARTIAL, access.permissions(HealthCategory.ACTIVITY).level)
            assertEquals(setOf("read.vo2"), access.permissions(HealthCategory.ACTIVITY).missing)
            assertEquals(listOf("steps", "vo2_max"), accessTypes(repo.recordTypes, HealthCategory.ACTIVITY, access.permissions(HealthCategory.ACTIVITY)).map { it.id })
            assertEquals(AccessLevel.NOT_REQUESTABLE, access.permissions(HealthCategory.SLEEP).level)
        } finally { scope.cancel() }
    }
    @Test fun `denial and cancellation recheck actual grants without manufacturing access`() {
        val scope = scope()
        try {
            val repo = Repository(); repo.env = repo.env.copy(grantedPermissions = emptySet())
            val access = AccessViewModel(HealthBrowserService(repo), scope)
            access.foreground(); access.refresh(); access.refresh()
            assertEquals(AccessLevel.NONE, access.permissions(HealthCategory.ACTIVITY).level)
            assertTrue(repo.requests.isEmpty())
        } finally { scope.cancel() }
    }
    @Test fun `provider recovery and environment errors are never empty data`() = runBlocking {
        val scope = scope()
        try {
            val repo = Repository(); repo.env = repo.env.copy(provider = ProviderStatus.UPDATE_REQUIRED)
            val access = AccessViewModel(HealthBrowserService(repo), scope); val live = live(repo, access, scope)
            access.foreground()
            live.uiState.await { it.state is RecordsState.ProviderUnavailable }
            assertTrue(repo.requests.isEmpty())
            repo.env = repo.env.copy(provider = ProviderStatus.AVAILABLE); access.refresh()
            live.uiState.await { it.records.isNotEmpty() }
            repo.failure = ReadFailure.PROVIDER_ERROR; access.refresh()
            assertTrue(live.uiState.await { it.state is RecordsState.Failed }.records.isEmpty())
        } finally { scope.cancel() }
    }
    @Test fun `revocation clears records and blocks subsequent reads`() = runBlocking {
        val scope = scope()
        try {
            val repo = Repository(); val access = AccessViewModel(HealthBrowserService(repo), scope); val live = live(repo, access, scope)
            access.foreground(); live.uiState.await { it.records.isNotEmpty() }
            val count = repo.requests.size
            access.background(); live.uiState.await { it.records.isEmpty() }
            repo.env = repo.env.copy(grantedPermissions = emptySet()); access.foreground()
            live.uiState.await { it.state is RecordsState.AccessRequired }
            assertEquals(count, repo.requests.size)
        } finally { scope.cancel() }
    }
    @Test fun `leaving foreground cancels an in flight read`() = runBlocking {
        val scope = scope()
        try {
            val repo = Repository(); val started = CompletableDeferred<Unit>(); val cancelled = CompletableDeferred<Unit>()
            repo.reader = { started.complete(Unit); try { awaitCancellation() } finally { cancelled.complete(Unit) } }
            val access = AccessViewModel(HealthBrowserService(repo), scope); val live = live(repo, access, scope)
            access.foreground(); withTimeout(3000) { started.await() }; access.background()
            withTimeout(3000) { cancelled.await() }
            assertTrue(live.uiState.value.records.isEmpty())
        } finally { scope.cancel() }
    }
    @Test fun `hidden route never resumes reads after access refresh`() = runBlocking {
        val scope = scope()
        try {
            val repo = Repository(); val access = AccessViewModel(HealthBrowserService(repo), scope); val live = live(repo, access, scope)
            access.foreground(); live.uiState.await { it.records.isNotEmpty() }; live.detach()
            live.uiState.await { it.records.isEmpty() }; val count = repo.requests.size
            access.background(); access.foreground(); yield()
            assertEquals(count, repo.requests.size)
        } finally { scope.cancel() }
    }
    @Test fun `history denial keeps recent subset and grant retries original range`() = runBlocking {
        val scope = scope()
        try {
            val repo = Repository(); val access = AccessViewModel(HealthBrowserService(repo), scope); val live = live(repo, access, scope, Period.DAYS_90)
            access.foreground()
            val recent = live.uiState.await { it.records.isNotEmpty() }
            assertEquals(now.minus(Duration.ofDays(30)), recent.recentWindow?.start)
            access.refresh(); live.uiState.await { it.records.isNotEmpty() }
            assertFalse(repo.requests.last().query.requireFullHistory)
            repo.env = repo.env.copy(historyGranted = true); access.refresh()
            live.uiState.await { it.records.isNotEmpty() && it.recentWindow == null }
            assertTrue(repo.requests.last().query.requireFullHistory)
            assertEquals(Instant.parse("2026-06-21T00:00:00Z"), repo.requests.last().query.range.start)
        } finally { scope.cancel() }
    }
    @Test fun `unsupported older range never reads or reports empty`() = runBlocking {
        val scope = scope()
        try {
            val repo = Repository(); repo.env = repo.env.copy(features = emptySet())
            val access = AccessViewModel(HealthBrowserService(repo), scope); val live = live(repo, access, scope)
            live.select(LiveSelection("steps", LocalDate.of(2025, 1, 1), Period.DAY)); access.foreground()
            assertEquals(RecordsState.UnsupportedFeature(HealthFeature.HISTORY), live.uiState.await { it.state !is RecordsState.Loading }.state)
            assertTrue(repo.requests.isEmpty())
        } finally { scope.cancel() }
    }
    @Test fun `paging deduplicates and permission race clears previous page`() = runBlocking {
        val scope = scope()
        try {
            val repo = Repository(); repo.reader = { request -> PageResult.Success(RecordPage(listOf(record), RecordCursor(request.query, "next"))) }
            val access = AccessViewModel(HealthBrowserService(repo), scope); val live = live(repo, access, scope)
            access.foreground(); live.uiState.await { it.records.isNotEmpty() }; live.loadMore()
            yield(); assertEquals(1, live.uiState.value.records.size)
            repo.reader = { PageResult.Failed(ReadFailure.ACCESS_CHANGED) }; live.loadMore()
            assertTrue(live.uiState.await { it.state is RecordsState.Failed }.records.isEmpty())
        } finally { scope.cancel() }
    }
    @Test fun `manifest permissions exactly match implemented readers and optional history`() {
        val file = java.io.File("src/main/AndroidManifest.xml").takeIf { it.exists() } ?: java.io.File("app/src/main/AndroidManifest.xml")
        val manifest = file.readText()
        val permissions = Regex("<uses-permission android:name=\"([^\"]+)\"").findAll(manifest).map { it.groupValues[1] }.toSet()
        val catalog = dev.zdrng.yealth.data.healthconnect.HealthRecordCatalog()
        val implemented = catalog.entries.filter { it.type.readerStatus == ReaderStatus.IMPLEMENTED }.map { it.type.readPermission }.toSet()
        assertEquals(implemented + "android.permission.health.READ_HEALTH_DATA_HISTORY", permissions)
        assertTrue(manifest.contains("android.permission.START_VIEW_PERMISSION_USAGE"))
        assertTrue(manifest.contains("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"))
    }
}
