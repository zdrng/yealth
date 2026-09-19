package dev.zdrng.yealth.domain.model

/** A provider aggregate is not a record and has no record timestamp or device. */
data class StepsTotal(val range: HealthTimeRange, val count: Long?, val origins: Set<String>)

sealed interface StepsTotalResult {
    data class Success(val total: StepsTotal) : StepsTotalResult
    data class Failed(val reason: ReadFailure) : StepsTotalResult
}

sealed interface StepsSummaryState {
    data object Loading : StepsSummaryState
    data class Available(val total: StepsTotal) : StepsSummaryState
    data class Unavailable(val reason: RecordsState) : StepsSummaryState
}
