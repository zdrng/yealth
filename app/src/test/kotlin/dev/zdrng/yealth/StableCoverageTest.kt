package dev.zdrng.yealth

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.*
import androidx.health.connect.client.response.ReadRecordsResponse
import dev.zdrng.yealth.data.healthconnect.*
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.service.HealthBrowserService
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class StableCoverageTest {
    private val catalog = HealthRecordCatalog()
    private class Gateway : HealthConnectGateway {
        var records = emptyList<Record>()
        var token: String? = null
        var permissions = emptySet<String>()
        var calls = 0
        var request: ReadRecordsRequest<*>? = null
        override fun sdkStatus() = HealthConnectClient.SDK_AVAILABLE
        override fun featureStatus(feature: Int) = HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        override suspend fun grantedPermissions() = permissions
        override suspend fun aggregate(request: AggregateRequest) = AggregationResult(emptyMap(), emptyMap(), emptySet())
        @Suppress("UNCHECKED_CAST") override suspend fun <T : Record> read(request: ReadRecordsRequest<T>): ReadRecordsResponse<T> {
            calls++; this.request = request
            return ReadRecordsResponse(records as List<T>, token)
        }
    }
    private fun query(id: String) = RecordQuery(id, HealthTimeRange(StableRecordFixtures.start, StableRecordFixtures.end), pageSize = 1)

    @Test fun `all forty stable adapters preserve every schema field time and metadata`() = runSuspend {
        val fixtures = StableRecordFixtures.all()
        assertEquals(catalog.entries.filter { it.type.readerStatus == ReaderStatus.IMPLEMENTED }.map { it.type.id }.toSet(), fixtures.keys)
        val gateway = Gateway(); val repo = HealthConnectRepository(gateway)
        for ((id, source) in fixtures) {
            gateway.records = listOf(source)
            val record = (repo.read(RecordRequest(query(id))) as PageResult.Success).page.records.single()
            val descriptor = repo.recordTypes.single { it.id == id }
            val common = setOf("time", "zoneOffset", "startTime", "endTime", "startZoneOffset", "endZoneOffset", "metadata")
            val fields = record.fields.map { it.key }.toMutableSet()
            if (descriptor.fields.any { it.key == "samples" }) { fields += "samples"; assertEquals(id, 1, record.samples.size) }
            if (descriptor.fields.any { it.key == "deltas" }) { fields += "deltas"; assertEquals(id, 1, record.samples.size) }
            assertEquals(id, descriptor.fields.map { it.key }.toSet() - common, fields)
            assertEquals(id, source.metadata.toDomain(), record.metadata)
            assertEquals(id, if (descriptor.timeShape == TimeShape.INSTANT) RecordTime.Point(StableRecordFixtures.start, StableRecordFixtures.offset)
                else RecordTime.Interval(StableRecordFixtures.start, StableRecordFixtures.end, StableRecordFixtures.offset, null), record.time)
            assertEquals(id, descriptor.readPermission, androidx.health.connect.client.permission.HealthPermission.getReadPermission(source::class))
        }
    }

    @Test fun `each stable reader gates access and supports empty results and exact cursor paging`() = runSuspend {
        val gateway = Gateway(); val repo = HealthConnectRepository(gateway); val service = HealthBrowserService(repo)
        for ((id, source) in StableRecordFixtures.all()) {
            gateway.permissions = emptySet(); val before = gateway.calls
            assertTrue(id, service.records(RecordRequest(query(id))) is RecordsState.AccessRequired)
            assertEquals(before, gateway.calls)
            gateway.permissions = setOf(repo.recordTypes.single { it.id == id }.readPermission)
            gateway.records = emptyList(); gateway.token = null
            assertTrue(id, service.records(RecordRequest(query(id))) is RecordsState.Empty)
            gateway.records = listOf(source); gateway.token = "next-$id"
            val page = (service.records(RecordRequest(query(id))) as RecordsState.Content).page
            assertEquals(RecordCursor(query(id), "next-$id"), page.next)
            gateway.records = emptyList(); gateway.token = null
            val tail = service.records(RecordRequest(query(id), page.next)) as RecordsState.Content
            assertNull(tail.page.next)
            assertEquals("next-$id", gateway.request!!.pageToken)
            assertEquals(source::class, gateway.request!!.recordType)
        }
    }

    @Test fun `manifest permissions exactly match stable implemented readers plus separate history`() {
        val file = File("src/main/AndroidManifest.xml").takeIf { it.exists() } ?: File("app/src/main/AndroidManifest.xml")
        val permissions = Regex("<uses-permission android:name=\"([^\"]+)\"").findAll(file.readText()).map { it.groupValues[1] }.toSet()
        val expected = catalog.entries.filter { it.type.readerStatus == ReaderStatus.IMPLEMENTED }.map { it.type.readPermission }.toSet() + "android.permission.health.READ_HEALTH_DATA_HISTORY"
        assertEquals(expected, permissions)
    }

    @Test fun `nutrition null and zero remain distinct and all nutrient units remain explicit`() {
        val base = StableRecordFixtures.all().getValue("nutrition") as NutritionRecord
        val mapped = mapNutrition(base)
        assertTrue(mapped.fields.filter { it.key !in setOf("mealType") }.all { it.value == HealthValue.Missing })
        val withZero = NutritionRecord(base.startTime, base.startZoneOffset, base.endTime, base.endZoneOffset, base.metadata,
            protein = androidx.health.connect.client.units.Mass.grams(0.0), vitaminB12 = androidx.health.connect.client.units.Mass.micrograms(3.5))
        val fields = mapNutrition(withZero).fields.associate { it.key to it.value }
        assertEquals(HealthValue.Decimal(0.0, "g"), fields["protein"])
        assertEquals(HealthValue.Decimal(0.0000035, "g"), fields["vitaminB12"])
        assertEquals(HealthValue.Missing, fields["sugar"])
    }

    @Test fun `nested sessions routes goals targets and samples retain original fields`() {
        val fixtures = StableRecordFixtures.all()
        val sleep = mapSleepSession(fixtures.getValue("sleep_session") as SleepSessionRecord)
        val stages = sleep.fields.single { it.key == "stages" }.value as HealthValue.Items
        assertEquals(setOf("startTime", "endTime", "stage"), (stages.values.single() as HealthValue.Fields).fields.map { it.key }.toSet())
        val exercise = mapExerciseSession(fixtures.getValue("exercise_session") as ExerciseSessionRecord)
        val route = exercise.fields.single { it.key == "exerciseRouteResult" }.value as HealthValue.Fields
        val point = ((route.fields.single { it.key == "locations" }.value as HealthValue.Items).values.single() as HealthValue.Fields).fields.associate { it.key to it.value }
        assertEquals(HealthValue.Decimal(52.0, "°"), point["latitude"])
        assertEquals(HealthValue.Missing, point["verticalAccuracy"])
        assertNotEquals(mapRouteResult(ExerciseRouteResult.NoData()), mapRouteResult(ExerciseRouteResult.ConsentRequired()))
        val plan = mapPlannedExerciseSession(fixtures.getValue("planned_exercise_session") as PlannedExerciseSessionRecord)
        assertTrue((plan.fields.single { it.key == "blocks" }.value as HealthValue.Items).values.isNotEmpty())
        val skin = mapSkinTemperature(fixtures.getValue("skin_temperature") as SkinTemperatureRecord)
        assertEquals(HealthValue.Decimal(-0.25, "°C"), skin.samples.single().fields.single().value)
    }
}
