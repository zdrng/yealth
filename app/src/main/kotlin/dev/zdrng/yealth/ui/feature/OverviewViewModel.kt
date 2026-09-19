package dev.zdrng.yealth.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.service.HealthBrowserService
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class OverviewSelection(val types: List<String>, val date: LocalDate, val period: Period)
data class OverviewUiState(
    val selection: OverviewSelection? = null,
    val accessRevision: Long = -1,
    val summaries: Map<String, MetricSummaryState> = emptyMap(),
    val records: Map<String, RecordsState> = emptyMap(),
    val recentOnly: Set<String> = emptySet(),
)

/** Screen-owned overview. Each category is bounded; hidden screens never fan out health reads. */
class OverviewViewModel(
    private val service: HealthBrowserService,
    access: StateFlow<AccessUiState>,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: ZoneId? = null,
    scope: CoroutineScope? = null,
) : ViewModel() {
    private val visible = MutableStateFlow(false)
    private val selection = MutableStateFlow<OverviewSelection?>(null)
    private val mutableState = MutableStateFlow(OverviewUiState())
    val uiState = mutableState.asStateFlow()
    init {
        (scope ?: viewModelScope).launch {
            combine(access, selection, visible) { status, chosen, attached -> status to chosen.takeIf { attached } }
                .collectLatest { (status, chosen) ->
                    mutableState.value = OverviewUiState(chosen, status.revision)
                    if (!status.foreground || chosen == null || status.result == null) return@collectLatest
                    for (id in chosen.types.distinct()) {
                        ensureActive()
                        val range = selectedRange(LiveSelection(id, chosen.date, chosen.period), status.environment?.historyGranted == true,
                            clock, zone ?: ZoneId.systemDefault())
                        if (id in SUMMARY_TYPES) {
                            val result = service.metricSummary(range.query)
                            ensureActive()
                            mutableState.value = mutableState.value.copy(summaries = mutableState.value.summaries + (id to result))
                        } else {
                            val result = service.records(RecordRequest(range.query.copy(pageSize = 1)))
                            ensureActive()
                            mutableState.value = mutableState.value.copy(records = mutableState.value.records + (id to result))
                        }
                        if (range.recentWindow != null) mutableState.value = mutableState.value.copy(recentOnly = mutableState.value.recentOnly + id)
                    }
                }
        }
    }
    fun attach() { visible.value = true }
    fun detach() { visible.value = false }
    fun select(value: OverviewSelection) { selection.value = value }
    companion object { val SUMMARY_TYPES = setOf("sleep_session", "heart_rate", "distance", "exercise_session") }
}
