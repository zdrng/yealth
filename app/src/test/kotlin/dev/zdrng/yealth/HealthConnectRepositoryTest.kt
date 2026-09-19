package dev.zdrng.yealth

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import dev.zdrng.yealth.data.healthconnect.*
import dev.zdrng.yealth.domain.model.*
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class HealthConnectRepositoryTest {
    private val start = Instant.parse("2026-09-18T08:00:00Z")
    private val end = start.plusSeconds(60)
    private val query = RecordQuery("steps", HealthTimeRange(start, end), pageSize = 25)
    private val metadata = Metadata.manualEntry("source-record", 7,
        Device(type = Device.TYPE_WATCH, manufacturer = "Fixture", model = "Test watch"))

    private class Gateway : HealthConnectGateway {
        var status = HealthConnectClient.SDK_AVAILABLE
        var features = emptySet<Int>()
        var permissions = emptySet<String>()
        var providerCalls = 0
        var reads = mutableListOf<ReadRecordsRequest<*>>()
        var result: ReadRecordsResponse<out Record> = ReadRecordsResponse(emptyList<StepsRecord>(), null)
        var failure: Exception? = null
        var aggregateResult = androidx.health.connect.client.aggregate.AggregationResult(emptyMap(), emptyMap(), emptySet())
        val aggregates = mutableListOf<androidx.health.connect.client.request.AggregateRequest>()
        override suspend fun aggregate(request: androidx.health.connect.client.request.AggregateRequest): androidx.health.connect.client.aggregate.AggregationResult {
            aggregates += request
            failure?.let { throw it }
            return aggregateResult
        }
        override fun sdkStatus(): Int = status
        override fun featureStatus(feature: Int): Int {
            providerCalls++
            return if (feature in features) HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
            else HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE
        }
        override suspend fun grantedPermissions(): Set<String> {
            providerCalls++
            failure?.let { throw it }
            return permissions
        }
        @Suppress("UNCHECKED_CAST")
        override suspend fun <T : Record> read(request: ReadRecordsRequest<T>): ReadRecordsResponse<T> {
            reads += request
            failure?.let { throw it }
            return result as ReadRecordsResponse<T>
        }
    }

    @Test fun `step total uses SDK aggregation and preserves null zero origins and range`() = runSuspend {
        val gateway = Gateway(); val repo = HealthConnectRepository(gateway)
        for (value in listOf(null, 0L, 6842L)) {
            gateway.aggregateResult = androidx.health.connect.client.aggregate.AggregationResult(
                value?.let { mapOf(StepsRecord.COUNT_TOTAL.metricKey to it) }.orEmpty(), emptyMap(),
                setOf(androidx.health.connect.client.records.metadata.DataOrigin("one"), androidx.health.connect.client.records.metadata.DataOrigin("two")))
            val result = repo.stepsTotal(query.range) as StepsTotalResult.Success
            assertEquals(StepsTotal(query.range, value, setOf("one", "two")), result.total)
        }
        assertEquals(androidx.health.connect.client.request.AggregateRequest(setOf(StepsRecord.COUNT_TOTAL),
            androidx.health.connect.client.time.TimeRangeFilter.between(start, end)), gateway.aggregates.last())
        assertTrue(gateway.reads.isEmpty())
    }

    @Test fun `aggregate errors redact provider messages and cancellation propagates`() = runSuspend {
        val gateway = Gateway(); val repo = HealthConnectRepository(gateway)
        for ((exception, reason) in listOf(SecurityException("private") to ReadFailure.ACCESS_CHANGED,
            IllegalArgumentException("private") to ReadFailure.INVALID_REQUEST, IllegalStateException("private") to ReadFailure.PROVIDER_ERROR)) {
            gateway.failure = exception
            assertEquals(StepsTotalResult.Failed(reason), repo.stepsTotal(query.range))
        }
        val cancelled = CancellationException("stop"); gateway.failure = cancelled
        assertSame(cancelled, assertThrows(CancellationException::class.java) { runSuspend { repo.stepsTotal(query.range) } })
    }

    @Test fun `overview aggregates use correct SDK metrics units and provenance`() = runSuspend {
        val gateway = Gateway(); val repo = HealthConnectRepository(gateway)
        gateway.aggregateResult = androidx.health.connect.client.aggregate.AggregationResult(
            mapOf(SleepSessionRecord.SLEEP_DURATION_TOTAL.metricKey to 26_640_000L,
                HeartRateRecord.BPM_AVG.metricKey to 72L, ExerciseSessionRecord.EXERCISE_DURATION_TOTAL.metricKey to 2_520_000L),
            mapOf(DistanceRecord.DISTANCE_TOTAL.metricKey to 4800.5),
            setOf(androidx.health.connect.client.records.metadata.DataOrigin("first"), androidx.health.connect.client.records.metadata.DataOrigin("second")))
        val expected = mapOf("sleep_session" to HealthValue.Decimal(444.0, "min"), "heart_rate" to HealthValue.Integer(72L, "bpm"),
            "exercise_session" to HealthValue.Decimal(42.0, "min"), "distance" to HealthValue.Decimal(4800.5, "m"))
        for ((id, value) in expected) {
            val result = repo.metricSummary(id, query.range) as MetricSummaryResult.Success
            assertEquals(value, result.summary.value)
            assertEquals(setOf("first", "second"), result.summary.origins)
            assertEquals(query.range, result.summary.range)
        }
        assertTrue(gateway.reads.isEmpty())
        gateway.aggregateResult = androidx.health.connect.client.aggregate.AggregationResult(emptyMap(), emptyMap(), emptySet())
        assertNull((repo.metricSummary("sleep_session", query.range) as MetricSummaryResult.Success).summary.value)
        gateway.failure = SecurityException("private record details")
        assertEquals(MetricSummaryResult.Failed(ReadFailure.ACCESS_CHANGED), repo.metricSummary("heart_rate", query.range))
    }

    @Test fun `unavailable and update required providers are not instantiated`() = runSuspend {
        val gateway = Gateway()
        val repository = HealthConnectRepository(gateway)
        for ((sdk, expected) in listOf(HealthConnectClient.SDK_UNAVAILABLE to ProviderStatus.UNAVAILABLE,
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED to ProviderStatus.UPDATE_REQUIRED)) {
            gateway.status = sdk
            assertEquals(expected, (repository.environment() as EnvironmentResult.Ready).environment.provider)
            assertEquals(0, gateway.providerCalls)
        }
        gateway.status = HealthConnectClient.SDK_AVAILABLE
        assertEquals(ProviderStatus.AVAILABLE, (repository.environment() as EnvironmentResult.Ready).environment.provider)
        assertTrue(gateway.providerCalls > 0)
    }

    @Test fun `history grant without feature support is not treated as usable`() = runSuspend {
        val gateway = Gateway()
        gateway.permissions = setOf(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)
        val repository = HealthConnectRepository(gateway)
        assertFalse((repository.environment() as EnvironmentResult.Ready).environment.historyGranted)
        gateway.features = setOf(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY)
        assertTrue((repository.environment() as EnvironmentResult.Ready).environment.historyGranted)
        gateway.permissions = emptySet()
        assertFalse((repository.environment() as EnvironmentResult.Ready).environment.historyGranted)
    }

    @Test fun `interval mapping preserves count offsets metadata and opaque paging`() = runSuspend {
        val gateway = Gateway()
        gateway.result = ReadRecordsResponse(listOf(StepsRecord(start, ZoneOffset.ofHours(2), end, null, 6842, metadata)), "opaque-next")
        val repository = HealthConnectRepository(gateway)
        val page = (repository.read(RecordRequest(query)) as PageResult.Success).page
        val record = page.records.single()
        assertEquals(RecordTime.Interval(start, end, ZoneOffset.ofHours(2), null), record.time)
        assertEquals(HealthValue.Integer(6842, "steps"), record.fields.single().value)
        assertEquals(metadata.id, record.metadata.id)
        assertEquals(metadata.dataOrigin.packageName, record.metadata.originPackage)
        assertEquals(metadata.lastModifiedTime, record.metadata.lastModified)
        assertEquals("source-record", record.metadata.clientRecordId)
        assertEquals(7L, record.metadata.clientRecordVersion)
        assertEquals(Metadata.RECORDING_METHOD_MANUAL_ENTRY, record.metadata.recordingMethod)
        assertEquals(HealthDevice(Device.TYPE_WATCH, "Fixture", "Test watch"), record.metadata.device)
        assertEquals(RecordCursor(query, "opaque-next"), page.next)
        repository.read(RecordRequest(query, page.next))
        val sent = gateway.reads.last()
        assertEquals(StepsRecord::class, sent.recordType)
        assertEquals(start, sent.timeRangeFilter.startTime)
        assertEquals(end, sent.timeRangeFilter.endTime)
        assertEquals(25, sent.pageSize)
        assertFalse(sent.ascendingOrder)
        assertEquals("opaque-next", sent.pageToken)
        assertTrue(sent.dataOriginFilter.isEmpty())
    }

    @Test fun `empty provider page token ends pagination`() = runSuspend {
        val gateway = Gateway()
        gateway.result = ReadRecordsResponse(emptyList<StepsRecord>(), "")
        val page = (HealthConnectRepository(gateway).read(RecordRequest(query)) as PageResult.Success).page
        assertNull(page.next)
    }

    @Test fun `instant measurement is not rounded or given an invented time zone`() = runSuspend {
        val gateway = Gateway()
        gateway.result = ReadRecordsResponse(listOf(Vo2MaxRecord(start, null, metadata, 43.212345,
            Vo2MaxRecord.MEASUREMENT_METHOD_COOPER_TEST)), null)
        val page = (HealthConnectRepository(gateway).read(RecordRequest(query.copy(typeId = "vo2_max"))) as PageResult.Success).page
        val record = page.records.single()
        assertEquals(RecordTime.Point(start, null), record.time)
        assertEquals(HealthValue.Decimal(43.212345, "mL/(min·kg)"), record.fields[0].value)
        assertEquals(HealthValue.Code(Vo2MaxRecord.MEASUREMENT_METHOD_COOPER_TEST), record.fields[1].value)
        assertNull(page.next)
    }

    @Test fun `series keeps every sample at its original timestamp without a derived average`() = runSuspend {
        val gateway = Gateway()
        gateway.result = ReadRecordsResponse(listOf(HeartRateRecord(start, null, end, null,
            listOf(HeartRateRecord.Sample(start.plusSeconds(1), 68), HeartRateRecord.Sample(start.plusSeconds(47), 71)), metadata)), null)
        val page = (HealthConnectRepository(gateway).read(RecordRequest(query.copy(typeId = "heart_rate"))) as PageResult.Success).page
        val record = page.records.single()
        assertTrue(record.fields.isEmpty())
        assertEquals(listOf(start.plusSeconds(1), start.plusSeconds(47)), record.samples.map { it.time })
        assertEquals(listOf(HealthValue.Integer(68, "bpm"), HealthValue.Integer(71, "bpm")),
            record.samples.map { it.fields.single().value })
    }

    @Test fun `planned readers and invalid types do not reach the SDK`() = runSuspend {
        val gateway = Gateway()
        val repository = HealthConnectRepository(gateway)
        assertEquals(PageResult.NotImplemented("mindfulness_session"), repository.read(RecordRequest(query.copy(typeId = "mindfulness_session"))))
        assertEquals(PageResult.Failed(ReadFailure.INVALID_REQUEST), repository.read(RecordRequest(query.copy(typeId = "unknown"))))
        assertTrue(gateway.reads.isEmpty())
    }

    @Test fun `permission race and provider errors stay typed without exposing exception messages`() = runSuspend {
        val gateway = Gateway()
        val repository = HealthConnectRepository(gateway)
        gateway.failure = SecurityException("private provider details")
        assertEquals(PageResult.Failed(ReadFailure.ACCESS_CHANGED), repository.read(RecordRequest(query)))
        assertEquals(EnvironmentResult.Failed(ReadFailure.ACCESS_CHANGED), repository.environment())
        gateway.failure = IllegalStateException("private provider details")
        assertEquals(PageResult.Failed(ReadFailure.PROVIDER_ERROR), repository.read(RecordRequest(query)))
        assertEquals(EnvironmentResult.Failed(ReadFailure.PROVIDER_ERROR), repository.environment())
    }

    @Test fun `cancellation propagates instead of becoming a failed read`() {
        val gateway = Gateway()
        val cancelled = CancellationException("cancelled")
        gateway.failure = cancelled
        val repository = HealthConnectRepository(gateway)
        assertSame(cancelled, assertThrows(CancellationException::class.java) {
            runSuspend { repository.environment() }
        })
        assertSame(cancelled, assertThrows(CancellationException::class.java) {
            runSuspend { repository.read(RecordRequest(query)) }
        })
    }
}
