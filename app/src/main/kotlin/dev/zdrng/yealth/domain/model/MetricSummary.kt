package dev.zdrng.yealth.domain.model

/** Provider-computed summaries retain their range and provenance, separate from raw records. */
data class MetricSummary(val typeId: String, val range: HealthTimeRange, val value: HealthValue?, val origins: Set<String>)
sealed interface MetricSummaryResult {
    data class Success(val summary: MetricSummary) : MetricSummaryResult
    data class Failed(val reason: ReadFailure) : MetricSummaryResult
}
sealed interface MetricSummaryState {
    data object Loading : MetricSummaryState
    data class Available(val summary: MetricSummary) : MetricSummaryState
    data class Unavailable(val reason: RecordsState) : MetricSummaryState
}
sealed interface DailyStepsState {
    data class Available(val totals: List<StepsTotal>) : DailyStepsState
    data class Unavailable(val reason: RecordsState) : DailyStepsState
}
