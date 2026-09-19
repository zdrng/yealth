package dev.zdrng.yealth.ui.feature

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.zdrng.yealth.R
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.ui.components.MetricValue
import dev.zdrng.yealth.ui.components.SummaryValue
import dev.zdrng.yealth.ui.components.HealthIcon
import dev.zdrng.yealth.ui.components.RecordedBar
import dev.zdrng.yealth.ui.components.RoundedBarChart
import dev.zdrng.yealth.ui.components.categoryCardColors
import java.text.NumberFormat
import dev.zdrng.yealth.ui.theme.LocalMockupColors
import dev.zdrng.yealth.ui.theme.Ink
import dev.zdrng.yealth.ui.theme.Electric
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun StepsSummaryCard(ui: StepsSummaryUiState, access: AccessUiState, selection: LiveSelection,
    open: (() -> Unit)?, retry: () -> Unit, tag: String, detailed: Boolean = false) {
    val current = ui.accessRevision == access.revision && ui.selection == selection && access.foreground
    val result = if (current) ui.state else StepsSummaryState.Loading
    if (!detailed && open != null) {
        CompactStepsSummary(result, ui.recentWindow != null, selection, open, retry, tag)
        return
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides Color.White) {
    Column(Modifier.fillMaxWidth().testTag(tag).background(Electric, MaterialTheme.shapes.extraLarge).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.health_connect_total), Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmallEmphasized, color = Color.White.copy(alpha = .85f))
            IconButton(retry, Modifier.testTag("refresh-summary")) {
                Icon(Icons.Default.Refresh, stringResource(R.string.refresh_summary))
            }
        }
        when (result) {
            StepsSummaryState.Loading -> LoadingIndicator(Modifier.size(48.dp).testTag("summary-loading"))
            is StepsSummaryState.Available -> {
                result.total.count?.let { count ->
                    MetricValue(NumberFormat.getIntegerInstance().format(count), Modifier.testTag("steps-total"), unit = stringResource(R.string.steps_unit))
                } ?: Text(stringResource(R.string.no_steps_total), style = MaterialTheme.typography.titleLarge)
                if (ui.recentWindow != null) Text(stringResource(R.string.aggregate_recent_only), style = MaterialTheme.typography.bodyMedium)
                if (selection.period == Period.WEEK && current) DailyStepsChart(ui)
                // Precise query bounds and provenance remain available after the measurement/chart.
                val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm XXX").withZone(ZoneId.systemDefault())
                Text(stringResource(R.string.aggregate_range, format.format(result.total.range.start), format.format(result.total.range.end)),
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .85f))
                Text(stringResource(R.string.aggregate_note), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .85f))
                if (result.total.origins.isNotEmpty()) Text(stringResource(R.string.aggregate_origins, result.total.origins.sorted().joinToString("\n")),
                    style = MaterialTheme.typography.bodySmall)
            }
            is StepsSummaryState.Unavailable -> Text(stringResource(when (val reason = result.reason) {
                is RecordsState.AccessRequired -> R.string.summary_access_needed
                is RecordsState.ProviderUnavailable -> if (reason.status == ProviderStatus.UPDATE_REQUIRED) R.string.provider_update else R.string.provider_unavailable
                RecordsState.HistoryAccessRequired -> R.string.history_limited
                is RecordsState.UnsupportedFeature -> R.string.history_unsupported
                is RecordsState.NotImplemented -> R.string.reader_planned
                is RecordsState.Failed -> if (reason.reason == ReadFailure.ACCESS_CHANGED) R.string.access_changed else R.string.read_failed
                else -> R.string.read_failed
            }), style = MaterialTheme.typography.bodyMedium)
        }
    }
    }
}

@Composable
private fun CompactStepsSummary(result: StepsSummaryState, recentOnly: Boolean, selection: LiveSelection,
    open: () -> Unit, retry: () -> Unit, tag: String) {
    Card(open, Modifier.fillMaxWidth().testTag(tag), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = LocalMockupColors.current.lilac, contentColor = Ink)) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HealthIcon(HealthCategory.ACTIVITY, prominent = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.type_steps), style = MaterialTheme.typography.titleMediumEmphasized)
                when (result) {
                    StepsSummaryState.Loading -> LoadingIndicator(Modifier.size(36.dp).testTag("summary-loading"))
                    is StepsSummaryState.Available -> Box(Modifier.testTag("steps-total")) {
                        SummaryValue(result.total.count?.let {
                            stringResource(R.string.steps_total_value, NumberFormat.getIntegerInstance().format(it))
                        } ?: stringResource(R.string.no_summary_value))
                    }
                    is StepsSummaryState.Unavailable -> Text(stringResource(when (result.reason) {
                        is RecordsState.AccessRequired -> R.string.summary_access_needed
                        is RecordsState.NotImplemented -> R.string.reader_planned
                        RecordsState.HistoryAccessRequired -> R.string.history_title
                        else -> R.string.summary_unavailable_short
                    }), style = MaterialTheme.typography.bodyMedium)
                }
                val format = DateTimeFormatter.ofPattern("d MMM")
                val start = selection.period.start(selection.date)
                val date = if (start == selection.date) format.format(start)
                    else stringResource(R.string.selected_range, format.format(start), format.format(selection.date))
                Text(stringResource(R.string.health_connect_total) + " · " + date, style = MaterialTheme.typography.labelSmall)
                if (recentOnly) Text(stringResource(R.string.recent_portion_short), style = MaterialTheme.typography.labelSmall)
            }
            IconButton(retry, Modifier.testTag("refresh-summary")) {
                Icon(Icons.Default.Refresh, stringResource(R.string.refresh_summary), Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DailyStepsChart(ui: StepsSummaryUiState) {
    if (ui.dailyLoading) LoadingIndicator(Modifier.size(40.dp))
    if (ui.dailyTotals.isNotEmpty()) {
        val zone = ZoneId.systemDefault()
        val short = DateTimeFormatter.ofPattern("EEEEE").withZone(zone)
        val full = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(zone)
        val number = NumberFormat.getIntegerInstance()
        val missing = stringResource(R.string.not_provided)
        val steps = stringResource(R.string.steps_unit)
        RoundedBarChart(ui.dailyTotals.map { total ->
            RecordedBar(short.format(total.range.start), total.count?.toDouble(),
                full.format(total.range.start) + ": " + (total.count?.let { number.format(it) + " " + steps } ?: missing))
        }, steps, Modifier.testTag("daily-steps-chart"))
        Text(stringResource(R.string.daily_chart_note), style = MaterialTheme.typography.bodySmall)
    }
    if (ui.dailyFailure != null) Text(stringResource(R.string.daily_chart_failed), style = MaterialTheme.typography.bodySmall)
}
