package dev.zdrng.yealth.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.service.HealthBrowserService
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class StepsSummaryUiState(
    val state: StepsSummaryState = StepsSummaryState.Loading,
    val selection: LiveSelection? = null,
    val recentWindow: HealthTimeRange? = null,
    val accessRevision: Long = -1,
    val dailyTotals: List<StepsTotal> = emptyList(),
    val dailyFailure: RecordsState? = null,
    val dailyLoading: Boolean = false,
)

/** Reads only aggregates for the visible summary. Never loads or sums raw pages. */
class StepsSummaryViewModel(
    private val service: HealthBrowserService,
    access: StateFlow<AccessUiState>,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: ZoneId? = null,
    scope: CoroutineScope? = null,
    private val includeDaily: Boolean = false,
) : ViewModel() {
    private val workScope = scope ?: viewModelScope
    private val visible = MutableStateFlow(false)
    private val selection = MutableStateFlow<LiveSelection?>(null)
    private val refresh = MutableStateFlow(0)
    private val mutableState = MutableStateFlow(StepsSummaryUiState())
    val uiState = mutableState.asStateFlow()
    init {
        workScope.launch {
            combine(access, selection, visible, refresh) { status, selected, attached, _ -> status to selected.takeIf { attached } }
                .collectLatest { (status, selected) ->
                    mutableState.value = StepsSummaryUiState(selection = selected, accessRevision = status.revision)
                    if (!status.foreground || selected == null || status.result == null) return@collectLatest
                    val environment = status.environment
                    if (environment == null) {
                        mutableState.value = mutableState.value.copy(state = StepsSummaryState.Unavailable(
                            RecordsState.Failed((status.result as EnvironmentResult.Failed).reason)))
                        return@collectLatest
                    }
                    val range = selectedRange(selected, environment.historyGranted, clock, zone ?: ZoneId.systemDefault())
                    val result = service.stepsSummary(range.query)
                    ensureActive()
                    mutableState.value = StepsSummaryUiState(result, selected, range.recentWindow, status.revision)
                    if (includeDaily && selected.period == Period.WEEK && result is StepsSummaryState.Available) {
                        mutableState.value = mutableState.value.copy(dailyLoading = true)
                        val currentZone = zone ?: ZoneId.systemDefault()
                        val bins = (0L..6L).mapNotNull { day ->
                            val date = selected.date.minusDays(6).plusDays(day)
                            val start = maxOf(date.atStartOfDay(currentZone).toInstant(), range.query.range.start)
                            val end = minOf(date.plusDays(1).atStartOfDay(currentZone).toInstant(), range.query.range.end)
                            if (start < end) HealthTimeRange(start, end) else null
                        }
                        val daily = service.dailySteps(range.query, bins)
                        ensureActive()
                        mutableState.value = when (daily) {
                            is DailyStepsState.Available -> mutableState.value.copy(dailyTotals = daily.totals, dailyLoading = false)
                            is DailyStepsState.Unavailable -> mutableState.value.copy(dailyFailure = daily.reason, dailyLoading = false)
                        }
                    }
                }
        }
    }
    fun attach() { visible.value = true }
    fun detach() { visible.value = false }
    fun select(value: LiveSelection) { selection.value = value }
    fun retry() { refresh.value++ }
}
