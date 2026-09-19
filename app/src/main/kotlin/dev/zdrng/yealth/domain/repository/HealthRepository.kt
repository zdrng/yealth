package dev.zdrng.yealth.domain.repository

import dev.zdrng.yealth.domain.model.MetricSummaryResult
import dev.zdrng.yealth.domain.model.ReadFailure
import dev.zdrng.yealth.domain.model.EnvironmentResult
import dev.zdrng.yealth.domain.model.PageResult
import dev.zdrng.yealth.domain.model.RecordRequest
import dev.zdrng.yealth.domain.model.RecordType
import dev.zdrng.yealth.domain.model.HealthTimeRange
import dev.zdrng.yealth.domain.model.StepsTotalResult

/** Read-only, foreground-invoked boundary. No platform classes cross this interface. */
interface HealthRepository {
    val recordTypes: List<RecordType>
    suspend fun environment(): EnvironmentResult
    suspend fun read(request: RecordRequest): PageResult
    suspend fun metricSummary(typeId: String, range: HealthTimeRange): MetricSummaryResult = MetricSummaryResult.Failed(ReadFailure.INVALID_REQUEST)
    suspend fun stepsTotal(range: HealthTimeRange): StepsTotalResult
}
