package dev.zdrng.yealth.data.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.repository.HealthRepository
import java.util.concurrent.CancellationException

class HealthConnectRepository(
    private val gateway: HealthConnectGateway,
    private val catalog: HealthRecordCatalog = HealthRecordCatalog(),
) : HealthRepository {
    override val recordTypes: List<RecordType> = catalog.entries.map { it.type }

    override suspend fun metricSummary(typeId: String, range: HealthTimeRange): MetricSummaryResult = try {
        val metric = when (typeId) {
            "sleep_session" -> SleepSessionRecord.SLEEP_DURATION_TOTAL
            "heart_rate" -> HeartRateRecord.BPM_AVG
            "distance" -> DistanceRecord.DISTANCE_TOTAL
            "exercise_session" -> ExerciseSessionRecord.EXERCISE_DURATION_TOTAL
            else -> null
        }
        if (metric == null) MetricSummaryResult.Failed(ReadFailure.INVALID_REQUEST)
        else {
            val result = gateway.aggregate(AggregateRequest(setOf(metric), TimeRangeFilter.between(range.start, range.end)))
            val value = when (typeId) {
                "sleep_session" -> result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.let { HealthValue.Decimal(it.seconds / 60.0 + it.nano / 60_000_000_000.0, "min") }
                "heart_rate" -> result[HeartRateRecord.BPM_AVG]?.let { HealthValue.Integer(it, "bpm") }
                "distance" -> result[DistanceRecord.DISTANCE_TOTAL]?.let { HealthValue.Decimal(it.inMeters, "m") }
                else -> result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.let { HealthValue.Decimal(it.seconds / 60.0 + it.nano / 60_000_000_000.0, "min") }
            }
            MetricSummaryResult.Success(MetricSummary(typeId, range, value, result.dataOrigins.map { it.packageName }.toSet()))
        }
    } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: SecurityException) { MetricSummaryResult.Failed(ReadFailure.ACCESS_CHANGED) }
      catch (_: IllegalArgumentException) { MetricSummaryResult.Failed(ReadFailure.INVALID_REQUEST) }
      catch (_: Exception) { MetricSummaryResult.Failed(ReadFailure.PROVIDER_ERROR) }

    override suspend fun stepsTotal(range: HealthTimeRange): StepsTotalResult = try {
        val response = gateway.aggregate(AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(range.start, range.end),
        ))
        StepsTotalResult.Success(StepsTotal(range, response[StepsRecord.COUNT_TOTAL], response.dataOrigins.map { it.packageName }.toSet()))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SecurityException) {
        StepsTotalResult.Failed(ReadFailure.ACCESS_CHANGED)
    } catch (_: IllegalArgumentException) {
        StepsTotalResult.Failed(ReadFailure.INVALID_REQUEST)
    } catch (_: Exception) {
        StepsTotalResult.Failed(ReadFailure.PROVIDER_ERROR)
    }

    @OptIn(ExperimentalMindfulnessSessionApi::class)
    override suspend fun environment(): EnvironmentResult = try {
        val provider = when (gateway.sdkStatus()) {
            HealthConnectClient.SDK_AVAILABLE -> ProviderStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> ProviderStatus.UPDATE_REQUIRED
            else -> ProviderStatus.UNAVAILABLE
        }
        if (provider != ProviderStatus.AVAILABLE) {
            EnvironmentResult.Ready(HealthEnvironment(provider))
        } else {
            val features = mapOf(
                HealthFeature.HISTORY to HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY,
                HealthFeature.SKIN_TEMPERATURE to HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
                HealthFeature.PLANNED_EXERCISE to HealthConnectFeatures.FEATURE_PLANNED_EXERCISE,
                HealthFeature.MINDFULNESS to HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION,
            ).filterValues { gateway.featureStatus(it) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE }.keys.toSet()
            val granted = gateway.grantedPermissions().toSet()
            EnvironmentResult.Ready(
                HealthEnvironment(
                    provider, features, granted,
                    HealthFeature.HISTORY in features && HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY in granted,
                ),
            )
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SecurityException) {
        EnvironmentResult.Failed(ReadFailure.ACCESS_CHANGED)
    } catch (_: Exception) {
        // Never forward exception messages containing record contents to UI or logs.
        EnvironmentResult.Failed(ReadFailure.PROVIDER_ERROR)
    }

    override suspend fun read(request: RecordRequest): PageResult = try {
        val adapter = catalog.entries.singleOrNull { it.type.id == request.query.typeId }
        if (adapter == null) PageResult.Failed(ReadFailure.INVALID_REQUEST)
        else adapter.read(gateway, request)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SecurityException) {
        PageResult.Failed(ReadFailure.ACCESS_CHANGED)
    } catch (_: IllegalArgumentException) {
        PageResult.Failed(ReadFailure.INVALID_REQUEST)
    } catch (_: Exception) {
        PageResult.Failed(ReadFailure.PROVIDER_ERROR)
    }
}
