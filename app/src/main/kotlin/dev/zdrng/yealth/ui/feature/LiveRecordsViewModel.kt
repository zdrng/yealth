package dev.zdrng.yealth.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.service.HealthBrowserService
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class LiveSelection(val type: String, val date: java.time.LocalDate, val period: Period)
data class LiveRecordsUiState(
    val state: RecordsState = RecordsState.Loading,
    val records: List<HealthRecord> = emptyList(),
    val recentWindow: HealthTimeRange? = null,
    val loadingMore: Boolean = false,
    val accessRevision: Long = -1,
    val selection: LiveSelection? = null,
)

/** Health values and cursors stay in memory and are cleared on every access recheck/stop. */
class LiveRecordsViewModel(
    private val service: HealthBrowserService,
    access: StateFlow<AccessUiState>,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: ZoneId? = null,
    scope: CoroutineScope? = null,
) : ViewModel() {
    private val workScope = scope ?: viewModelScope
    private val visible = MutableStateFlow(0)
    private val selection = MutableStateFlow<LiveSelection?>(null)
    private val refresh = MutableStateFlow(0)
    private val mutableState = MutableStateFlow(LiveRecordsUiState())
    val uiState = mutableState.asStateFlow()
    private var pageJob: Job? = null
    private var cursor: RecordCursor? = null
    private var generation = 0
    init {
        workScope.launch {
            combine(access, selection, refresh, visible) { status, query, revision, count ->
                Triple(status, query.takeIf { count > 0 }, revision)
            }.distinctUntilChanged().collectLatest { (status, selected, _) ->
                generation++
                pageJob?.cancel()
                cursor = null
                mutableState.value = LiveRecordsUiState(accessRevision = status.revision, selection = selected)
                if (!status.foreground || selected == null || status.result == null) return@collectLatest
                val env = status.environment
                if (env == null) {
                    mutableState.value = LiveRecordsUiState(RecordsState.Failed((status.result as EnvironmentResult.Failed).reason), accessRevision = status.revision, selection = selected)
                    return@collectLatest
                }
                val range = selectedRange(selected, env.historyGranted, clock, zone ?: ZoneId.systemDefault())
                val result = service.records(RecordRequest(range.query))
                ensureActive()
                publish(result, range.recentWindow)
            }
        }
    }
    fun attach() { visible.value++ }
    fun detach() { visible.value = (visible.value - 1).coerceAtLeast(0) }
    fun select(value: LiveSelection) { selection.value = value }
    fun retry() { refresh.value++ }
    fun loadMore() {
        val next = cursor ?: return
        if (pageJob?.isActive == true) return
        val ticket = generation
        mutableState.value = uiState.value.copy(loadingMore = true)
        pageJob = workScope.launch {
            val result = service.records(RecordRequest(next.query, next))
            ensureActive()
            if (ticket == generation) publish(result, uiState.value.recentWindow, append = true)
        }
    }
    private fun publish(result: RecordsState, recent: HealthTimeRange?, append: Boolean = false) {
        cursor = (result as? RecordsState.Content)?.page?.next
        val records = if (result is RecordsState.Content) {
            ((if (append) uiState.value.records else emptyList()) + result.page.records).distinctBy { it.metadata.id }
        } else emptyList()
        mutableState.value = LiveRecordsUiState(result, records, recent, accessRevision = uiState.value.accessRevision, selection = uiState.value.selection)
    }
}
