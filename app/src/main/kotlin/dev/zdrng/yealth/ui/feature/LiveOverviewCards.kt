package dev.zdrng.yealth.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import dev.zdrng.yealth.R
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.ui.components.MetricSummaryCard
import dev.zdrng.yealth.ui.components.MetricValue
import java.text.NumberFormat

/** Uses the same provider aggregate as the overview, now prominent on the metric page. */
@Composable
fun LiveMetricSummary(typeId: String, ui: OverviewUiState, access: AccessUiState, selection: OverviewSelection) {
    val current = access.foreground && ui.accessRevision == access.revision && ui.selection == selection
    val summary = ui.summaries[typeId].takeIf { current }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("metric-summary"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(if (typeId == "heart_rate") R.string.provider_average else R.string.health_connect_total),
            style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (summary) {
            is MetricSummaryState.Available -> {
                val value = summary.summary.value
                val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
                when (value) {
                    is HealthValue.Integer -> MetricValue(number.format(value.value), Modifier.testTag("metric-summary-value"), unit = value.unit.orEmpty())
                    is HealthValue.Decimal -> MetricValue(number.format(value.value), Modifier.testTag("metric-summary-value"), unit = value.unit)
                    else -> Text(stringResource(R.string.no_summary_value), style = MaterialTheme.typography.titleLarge)
                }
                if (typeId in ui.recentOnly) Text(stringResource(R.string.aggregate_recent_only), style = MaterialTheme.typography.bodyMedium)
                if (summary.summary.origins.isNotEmpty()) Text(stringResource(R.string.aggregate_origins, summary.summary.origins.sorted().joinToString("\n")),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is MetricSummaryState.Unavailable -> Text(overviewFailure(summary.reason))
            else -> LoadingIndicator(Modifier.size(48.dp))
        }
    }
}

@Composable
fun LiveOverviewCard(typeId: String, ui: OverviewUiState, access: AccessUiState, selection: OverviewSelection, open: () -> Unit, tag: String) {
    val current = access.foreground && ui.accessRevision == access.revision && ui.selection == selection
    val summary = ui.summaries[typeId].takeIf { current }
    val records = ui.records[typeId].takeIf { current }
    val failure = (summary as? MetricSummaryState.Unavailable)?.reason ?: records?.takeUnless { it is RecordsState.Content || it is RecordsState.Empty || it is RecordsState.Loading }
    val value = when {
        summary is MetricSummaryState.Available -> summary.summary.value?.let(::summaryValue) ?: stringResource(R.string.no_summary_value)
        records is RecordsState.Content -> records.page.records.firstOrNull()?.let { recordValue(it) } ?: stringResource(R.string.more_records_available)
        records is RecordsState.Empty -> stringResource(R.string.no_records)
        failure != null -> overviewFailure(failure)
        else -> null
    }
    val supporting = when {
        summary is MetricSummaryState.Available -> stringResource(if (typeId == "heart_rate") R.string.provider_average else R.string.health_connect_total)
        records is RecordsState.Content -> {
            val count = records.page.records.size
            pluralStringResource(if (records.page.next != null) R.plurals.overview_partial_count else R.plurals.overview_complete_count, count, count) +
                records.page.records.firstOrNull()?.let { " · " + it.metadata.originPackage }.orEmpty()
        }
        failure != null -> stringResource(R.string.open_records)
        records is RecordsState.Empty -> stringResource(R.string.selected_date_empty)
        else -> ""
    } + if (typeId in ui.recentOnly && current) " · " + stringResource(R.string.recent_portion_short) else ""
    MetricSummaryCard(typeId, stringResource(typeLabels.getValue(typeId)), value, supporting, open,
        Modifier.testTag(tag), loading = value == null, error = failure != null)
}

private fun summaryValue(value: HealthValue): String {
    val numbers = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    return when (value) {
        is HealthValue.Integer -> "${numbers.format(value.value)} ${value.unit.orEmpty()}"
        is HealthValue.Decimal -> "${numbers.format(value.value)} ${value.unit}"
        else -> "—"
    }
}
@Composable
private fun overviewFailure(state: RecordsState): String = stringResource(when (state) {
    is RecordsState.AccessRequired -> R.string.access_required
    is RecordsState.ProviderUnavailable -> if (state.status == ProviderStatus.UPDATE_REQUIRED) R.string.provider_update else R.string.provider_unavailable
    is RecordsState.UnsupportedFeature -> R.string.unsupported_feature
    is RecordsState.NotImplemented -> R.string.reader_planned
    RecordsState.HistoryAccessRequired -> R.string.history_limited
    else -> R.string.read_failed
})
