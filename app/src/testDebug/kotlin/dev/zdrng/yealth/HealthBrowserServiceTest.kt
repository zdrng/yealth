package dev.zdrng.yealth

import dev.zdrng.yealth.data.fake.FakeHealthRepository
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.service.HealthBrowserService
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class HealthBrowserServiceTest {
    private val type = RecordType("steps", HealthCategory.ACTIVITY, TimeShape.INTERVAL,
        "read.steps", emptyList(), readerStatus = ReaderStatus.IMPLEMENTED)
    private val range = HealthTimeRange(Instant.parse("2026-09-18T00:00:00Z"), Instant.parse("2026-09-19T00:00:00Z"))
    private val query = RecordQuery(type.id, range)
    private val request = RecordRequest(query)
    private val available = HealthEnvironment(ProviderStatus.AVAILABLE, grantedPermissions = setOf("read.steps"))
    private fun fake(environment: HealthEnvironment = available, types: List<RecordType> = listOf(type)) =
        FakeHealthRepository(types, EnvironmentResult.Ready(environment))

    @Test fun `missing access does not read or pretend to be empty`() = runSuspend {
        val repository = fake(available.copy(grantedPermissions = emptySet()))
        val result = HealthBrowserService(repository).records(request) as RecordsState.AccessRequired
        assertEquals(setOf("read.steps"), result.permissions.missing)
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun `provider states block all reads`() = runSuspend {
        for (status in listOf(ProviderStatus.UNAVAILABLE, ProviderStatus.UPDATE_REQUIRED)) {
            val repository = fake(available.copy(provider = status))
            assertEquals(RecordsState.ProviderUnavailable(status), HealthBrowserService(repository).records(request))
            assertTrue(repository.requests.isEmpty())
        }
    }

    @Test fun `feature gates are distinct from implementation gaps`() = runSuspend {
        val repository = fake(types = listOf(type.copy(feature = HealthFeature.SKIN_TEMPERATURE)))
        assertEquals(RecordsState.UnsupportedFeature(HealthFeature.SKIN_TEMPERATURE),
            HealthBrowserService(repository).records(request))
        val planned = fake(types = listOf(type.copy(readerStatus = ReaderStatus.PLANNED)))
        assertEquals(RecordsState.NotImplemented(type.id), HealthBrowserService(planned).records(request))
        assertTrue(repository.requests.isEmpty())
        assertTrue(planned.requests.isEmpty())
    }

    @Test fun `partial category access excludes planned and unsupported readers`() {
        val types = listOf(type, type.copy(id = "distance", readPermission = "read.distance"),
            type.copy(id = "planned", readPermission = "read.planned", readerStatus = ReaderStatus.PLANNED),
            type.copy(id = "gated", readPermission = "read.gated", feature = HealthFeature.PLANNED_EXERCISE))
        val state = HealthBrowserService(fake(types = types)).permissionsFor(HealthCategory.ACTIVITY, available)
        assertEquals(AccessLevel.PARTIAL, state.level)
        assertEquals(setOf("read.distance"), state.missing)
        assertEquals(AccessLevel.NOT_REQUESTABLE,
            HealthBrowserService(fake(types = types)).permissionsFor(HealthCategory.WELLNESS, available).level)
    }

    @Test fun `revocation after a successful read requires access on refresh`() = runSuspend {
        val repository = fake()
        repository.pages[request] = PageResult.Success(RecordPage(emptyList(), null))
        val service = HealthBrowserService(repository)
        assertEquals(RecordsState.Empty(historyLimited = true), service.records(request))
        repository.environmentResult = EnvironmentResult.Ready(available.copy(grantedPermissions = emptySet()))
        assertTrue(service.records(request) is RecordsState.AccessRequired)
        assertEquals(1, repository.requests.size)
    }

    @Test fun `history requires both a supported feature and a grant`() = runSuspend {
        val historyRequest = RecordRequest(query.copy(requireFullHistory = true))
        val repository = fake()
        val service = HealthBrowserService(repository)
        assertEquals(RecordsState.UnsupportedFeature(HealthFeature.HISTORY), service.records(historyRequest))
        repository.environmentResult = EnvironmentResult.Ready(available.copy(features = setOf(HealthFeature.HISTORY)))
        assertEquals(RecordsState.HistoryAccessRequired, service.records(historyRequest))
        assertTrue(repository.requests.isEmpty())
        repository.environmentResult = EnvironmentResult.Ready(available.copy(features = setOf(HealthFeature.HISTORY), historyGranted = true))
        repository.pages[historyRequest] = PageResult.Success(RecordPage(emptyList(), null))
        assertEquals(RecordsState.Empty(historyLimited = false), service.records(historyRequest))
    }

    @Test fun `empty intermediate and final continuation pages remain page content`() = runSuspend {
        val repository = fake()
        val cursor = RecordCursor(query, "next")
        repository.pages[request] = PageResult.Success(RecordPage(emptyList(), cursor))
        val nextRequest = RecordRequest(query, cursor)
        repository.pages[nextRequest] = PageResult.Success(RecordPage(emptyList(), null))
        val service = HealthBrowserService(repository)
        val initial = service.records(request) as RecordsState.Content
        assertEquals(cursor, initial.page.next)
        assertTrue(service.records(nextRequest) is RecordsState.Content)
    }

    @Test fun `errors never become empty data and unknown IDs never read`() = runSuspend {
        val repository = fake()
        repository.pages[request] = PageResult.Failed(ReadFailure.ACCESS_CHANGED)
        val service = HealthBrowserService(repository)
        assertEquals(RecordsState.Failed(ReadFailure.ACCESS_CHANGED), service.records(request))
        repository.environmentResult = EnvironmentResult.Failed(ReadFailure.PROVIDER_ERROR)
        assertEquals(RecordsState.Failed(ReadFailure.PROVIDER_ERROR), service.records(request))
        assertEquals(RecordsState.Failed(ReadFailure.INVALID_REQUEST), service.records(RecordRequest(query.copy(typeId = "unknown"))))
        assertEquals(1, repository.requests.size)
    }

    @Test fun `pagination tokens cannot leak across changed queries`() {
        val cursor = RecordCursor(query, "opaque")
        for (changed in listOf(query.copy(typeId = "other"), query.copy(ascending = true),
            query.copy(pageSize = 10), query.copy(requireFullHistory = true),
            query.copy(range = range.copy(end = range.end.plusSeconds(1))))) {
            assertThrows(IllegalArgumentException::class.java) { RecordRequest(changed, cursor) }
        }
        assertThrows(IllegalArgumentException::class.java) { query.copy(pageSize = 0) }
        assertThrows(IllegalArgumentException::class.java) { HealthTimeRange(range.end, range.start) }
    }
}
