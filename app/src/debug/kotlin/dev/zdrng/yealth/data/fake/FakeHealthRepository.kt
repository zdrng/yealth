package dev.zdrng.yealth.data.fake

import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.repository.HealthRepository

/** Explicit fixtures for previews/tests only. This source set is absent from release builds. */
class FakeHealthRepository(
    override val recordTypes: List<RecordType>,
    var environmentResult: EnvironmentResult,
    val pages: MutableMap<RecordRequest, PageResult> = mutableMapOf(),
) : HealthRepository {
    val requests = mutableListOf<RecordRequest>()
    override suspend fun stepsTotal(range: HealthTimeRange): StepsTotalResult = StepsTotalResult.Failed(ReadFailure.INVALID_REQUEST)
    override suspend fun environment(): EnvironmentResult = environmentResult
    override suspend fun read(request: RecordRequest): PageResult {
        requests += request
        return pages[request] ?: PageResult.Failed(ReadFailure.INVALID_REQUEST)
    }
}
