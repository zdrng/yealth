package dev.zdrng.yealth.ui.feature

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dev.zdrng.yealth.domain.model.*
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Catalog/navigation input. Samples exist only in the explicitly selected debug preview.
 * Live records are owned by LiveRecordsViewModel and never copied into this snapshot.
 */
data class BrowserContent(
    val catalog: List<RecordType>,
    val records: List<HealthRecord> = emptyList(),
    val isPreview: Boolean = false,
    val initialDate: LocalDate = LocalDate.now(),
)

enum class Period(val days: Long) {
    DAY(1), WEEK(7), MONTH(0), DAYS_7(7), DAYS_30(30), DAYS_90(90), YEAR(0);

    fun start(endInclusive: LocalDate): LocalDate = when (this) {
        MONTH -> endInclusive.minusMonths(1).plusDays(1)
        YEAR -> endInclusive.minusYears(1).plusDays(1)
        else -> endInclusive.minusDays(days - 1)
    }

    fun shift(date: LocalDate, direction: Long): LocalDate = when (this) {
        MONTH -> date.plusMonths(direction)
        YEAR -> date.plusYears(direction)
        else -> date.plusDays(days * direction)
    }
}

data class BrowserUiState(
    val date: LocalDate,
    val period: Period,
    val query: String = "",
    val records: List<HealthRecord> = emptyList(),
)

/** One instance per navigation entry. Saves selection only, never health values or page tokens. */
class BrowserViewModel(
    val content: BrowserContent,
    private val savedState: SavedStateHandle,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val initialPeriod = savedState.get<String>("period")?.let {
        runCatching { Period.valueOf(it) }.getOrNull()
    } ?: Period.DAY
    private val initialDate = savedState.get<String>("date")?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    } ?: content.initialDate
    private val mutableState = MutableStateFlow(buildState(initialDate, initialPeriod, savedState["query"] ?: ""))
    val uiState = mutableState.asStateFlow()

    fun selectDate(date: LocalDate) = update(uiState.value.copy(date = date))
    fun selectPeriod(period: Period) = update(uiState.value.copy(period = period))
    fun moveDate(direction: Long) = selectDate(uiState.value.period.shift(uiState.value.date, direction))
    fun search(query: String) = update(uiState.value.copy(query = query))

    fun record(id: String): HealthRecord? = content.records.singleOrNull { it.metadata.id == id }

    private fun update(state: BrowserUiState) {
        savedState["date"] = state.date.toString()
        savedState["period"] = state.period.name
        savedState["query"] = state.query
        mutableState.value = buildState(state.date, state.period, state.query)
    }

    private fun buildState(date: LocalDate, period: Period, query: String): BrowserUiState {
        val start = period.start(date).atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        // Lossless display filtering: intervals overlap the window, instants belong to [start, end).
        val records = content.records.filter { record ->
            when (val time = record.time) {
                is RecordTime.Point -> time.time >= start && time.time < end
                is RecordTime.Interval -> time.start < end && time.end > start
            }
        }.sortedByDescending { it.startTime() }
        return BrowserUiState(date, period, query, records)
    }
}

fun HealthRecord.startTime() = when (val t = time) {
    is RecordTime.Point -> t.time
    is RecordTime.Interval -> t.start
}

fun filterCatalog(catalog: List<RecordType>, query: String, labels: Map<String, String>, categories: Map<HealthCategory, String>): List<RecordType> {
    val normalized = query.trim()
    return catalog.filter { normalized.isEmpty() || labels.getValue(it.id).contains(normalized, ignoreCase = true) ||
        categories.getValue(it.category).contains(normalized, ignoreCase = true) }
}
