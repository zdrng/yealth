package dev.zdrng.yealth.domain.service

import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.repository.HealthRepository

class HealthBrowserService(private val repository: HealthRepository) {
    val recordTypes: List<RecordType> get() = repository.recordTypes

    suspend fun environment(): EnvironmentResult = repository.environment()

    fun permissionsFor(category: HealthCategory, environment: HealthEnvironment): PermissionState {
        val permissions = recordTypes.filter {
            it.category == category && it.readerStatus == ReaderStatus.IMPLEMENTED &&
                (it.feature == null || it.feature in environment.features)
        }.map { it.readPermission }.toSet()
        return PermissionState(permissions, environment.grantedPermissions intersect permissions)
    }

    suspend fun records(request: RecordRequest): RecordsState {
        val checked = repository.environment()
        accessFailure(request.query, checked)?.let { return it }
        val environment = (checked as EnvironmentResult.Ready).environment
        return when (val result = repository.read(request)) {
            is PageResult.NotImplemented -> RecordsState.NotImplemented(result.typeId)
            is PageResult.Failed -> RecordsState.Failed(result.reason)
            is PageResult.Success -> {
                val limited = !environment.historyGranted
                if (result.page.records.isEmpty() && result.page.next == null && request.cursor == null) {
                    RecordsState.Empty(limited)
                } else RecordsState.Content(result.page, limited)
            }
        }
    }

    suspend fun stepsSummary(query: RecordQuery): StepsSummaryState {
        if (query.typeId != "steps") return StepsSummaryState.Unavailable(RecordsState.Failed(ReadFailure.INVALID_REQUEST))
        accessFailure(query, repository.environment())?.let { return StepsSummaryState.Unavailable(it) }
        return when (val result = repository.stepsTotal(query.range)) {
            is StepsTotalResult.Success -> StepsSummaryState.Available(result.total)
            is StepsTotalResult.Failed -> StepsSummaryState.Unavailable(RecordsState.Failed(result.reason))
        }
    }

    suspend fun metricSummary(query: RecordQuery): MetricSummaryState {
        if (query.typeId !in setOf("sleep_session", "heart_rate", "distance", "exercise_session"))
            return MetricSummaryState.Unavailable(RecordsState.Failed(ReadFailure.INVALID_REQUEST))
        accessFailure(query, repository.environment())?.let { return MetricSummaryState.Unavailable(it) }
        return when (val result = repository.metricSummary(query.typeId, query.range)) {
            is MetricSummaryResult.Success -> MetricSummaryState.Available(result.summary)
            is MetricSummaryResult.Failed -> MetricSummaryState.Unavailable(RecordsState.Failed(result.reason))
        }
    }

    /** At most seven exact day ranges. Provider totals avoid double counting raw source records. */
    suspend fun dailySteps(query: RecordQuery, ranges: List<HealthTimeRange>): DailyStepsState {
        if (query.typeId != "steps" || ranges.size !in 1..7 || ranges.any { it.start < query.range.start || it.end > query.range.end } ||
            ranges.zipWithNext().any { (a, b) -> a.end != b.start })
            return DailyStepsState.Unavailable(RecordsState.Failed(ReadFailure.INVALID_REQUEST))
        val totals = mutableListOf<StepsTotal>()
        for (range in ranges) {
            // Recheck access between provider calls; stop at the first failure, never fill missing days with zero.
            when (val result = stepsSummary(query.copy(range = range))) {
                is StepsSummaryState.Available -> totals += result.total
                is StepsSummaryState.Unavailable -> return DailyStepsState.Unavailable(result.reason)
                StepsSummaryState.Loading -> error("Service does not emit loading")
            }
        }
        return DailyStepsState.Available(totals)
    }

    private fun accessFailure(query: RecordQuery, checked: EnvironmentResult): RecordsState? {
        val type = recordTypes.singleOrNull { it.id == query.typeId }
            ?: return RecordsState.Failed(ReadFailure.INVALID_REQUEST)
        val environment = when (val result = checked) {
            is EnvironmentResult.Ready -> result.environment
            is EnvironmentResult.Failed -> return RecordsState.Failed(result.reason)
        }
        if (environment.provider != ProviderStatus.AVAILABLE) {
            return RecordsState.ProviderUnavailable(environment.provider)
        }
        if (type.feature != null && type.feature !in environment.features) {
            return RecordsState.UnsupportedFeature(type.feature)
        }
        if (type.readerStatus != ReaderStatus.IMPLEMENTED) return RecordsState.NotImplemented(type.id)
        val access = PermissionState(setOf(type.readPermission), environment.grantedPermissions)
        if (access.missing.isNotEmpty()) return RecordsState.AccessRequired(access)
        if (query.requireFullHistory && !environment.historyGranted) {
            return if (HealthFeature.HISTORY in environment.features) RecordsState.HistoryAccessRequired
            else RecordsState.UnsupportedFeature(HealthFeature.HISTORY)
        }
        return null
    }
}
