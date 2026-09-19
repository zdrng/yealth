package dev.zdrng.yealth.ui.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import dev.zdrng.yealth.ui.theme.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import dev.zdrng.yealth.R
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.ui.components.*

@Composable
fun LiveAccessScreen(content: BrowserContent, state: AccessUiState, permissions: (HealthCategory) -> PermissionState,
    explain: (HealthCategory) -> Unit, history: () -> Unit, settings: () -> Unit, recover: () -> Unit, retry: () -> Unit) {
    val featured = listOf(HealthCategory.ACTIVITY, HealthCategory.SLEEP, HealthCategory.VITALS, HealthCategory.NUTRITION)
    val granted = HealthCategory.entries.count { permissions(it).level == AccessLevel.GRANTED }
    ScreenList("screen-access") {
        item { Heading(stringResource(R.string.access), artwork = HeadingArtwork.Access) }
        item { Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(if (granted == 0) R.string.choose_categories_share else if (granted == HealthCategory.entries.size) R.string.all_categories_shared else R.string.some_categories_shared),
                style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.access_short_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { ProviderPanel(state, recover, retry) }
        if (state.environment?.provider == ProviderStatus.AVAILABLE) {
            items(featured) { category -> LiveAccessCategory(category, permissions(category), explain) }
            item { Button(settings, Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("health-settings")) { Text(stringResource(R.string.health_settings)) } }
            item { NoteCard("", stringResource(R.string.offline_note)) }
            items(HealthCategory.entries.filterNot(featured::contains)) { category -> LiveAccessCategory(category, permissions(category), explain) }
            item { HistoryPanel(state.environment!!, history) }
        }
    }
}

@Composable
private fun LiveAccessCategory(category: HealthCategory, access: PermissionState, explain: (HealthCategory) -> Unit) {
    val color = when (category) { HealthCategory.SLEEP -> LocalMockupColors.current.mint; HealthCategory.VITALS -> LocalMockupColors.current.lemon; else -> LocalMockupColors.current.lilac }
    Card(onClick = { explain(category) }, modifier = Modifier.fillMaxWidth().testTag("access-${category.name}"),
        shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = color, contentColor = Ink)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HealthIcon(category)
            Column(Modifier.weight(1f)) {
                Text(stringResource(categoryLabel(category)), style = MaterialTheme.typography.titleMediumEmphasized)
                Text(stringResource(accessLabel(access.level)), style = MaterialTheme.typography.bodyMedium)
            }
            Box(Modifier.size(18.dp).background(if (access.level == AccessLevel.GRANTED) Color(0xFF39BA81) else Ink.copy(alpha = .2f), CircleShape))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

fun accessLabel(level: AccessLevel): Int = when (level) {
    AccessLevel.GRANTED -> R.string.access_granted
    AccessLevel.PARTIAL -> R.string.access_partial
    AccessLevel.NONE -> R.string.access_none
    AccessLevel.NOT_REQUESTABLE -> R.string.reader_planned
}

@Composable
fun ProviderPanel(state: AccessUiState, recover: () -> Unit, retry: () -> Unit) {
    when {
        state.result == null -> LoadingIndicator(Modifier.testTag("access-loading"))
        state.result is EnvironmentResult.Failed -> StateMessage(R.string.access_check_failed, retry, R.string.retry)
        state.environment?.provider == ProviderStatus.UPDATE_REQUIRED -> StateMessage(R.string.provider_update, recover, R.string.provider_recover, retry)
        state.environment?.provider == ProviderStatus.UNAVAILABLE -> StateMessage(R.string.provider_unavailable, recover, R.string.provider_recover, retry)
    }
}

@Composable
fun HistoryPanel(environment: HealthEnvironment, request: () -> Unit) {
    val supported = HealthFeature.HISTORY in environment.features
    NoteCard(stringResource(R.string.history_title), stringResource(when {
        environment.historyGranted -> R.string.history_granted
        !supported -> R.string.history_unsupported
        else -> R.string.history_limited
    }))
    if (supported && !environment.historyGranted && environment.grantedPermissions.isNotEmpty()) {
        TextButton(request, Modifier.testTag("history-request")) { Text(stringResource(R.string.history_request)) }
    }
}

@Composable
fun PermissionExplanation(category: HealthCategory?, types: List<RecordType>, history: Boolean,
    canRequest: Boolean, dismiss: () -> Unit, request: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss,
        title = { Text(stringResource(if (history) R.string.history_title else R.string.access_title)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (history) Text(stringResource(R.string.history_rationale))
            else {
                category?.let { Text(stringResource(categoryLabel(it))) }
                Text(stringResource(if (types.isEmpty()) R.string.reader_planned else R.string.category_rationale))
                types.forEach { Text(stringResource(typeLabels.getValue(it.id))) }
            }
            Text(stringResource(R.string.offline_note))
        } },
        confirmButton = { if (canRequest) TextButton(request, Modifier.testTag("permission-continue")) { Text(stringResource(R.string.continue_permission)) } },
        dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun LiveRecordsPanel(type: RecordType, access: AccessUiState, live: LiveRecordsUiState,
    request: () -> Unit, history: () -> Unit, recover: () -> Unit, settings: () -> Unit, refresh: () -> Unit,
    retry: () -> Unit, loadMore: () -> Unit, chooseDate: () -> Unit, openRecord: (String) -> Unit, showRows: Boolean = true) {
    if (access.environment?.provider != ProviderStatus.AVAILABLE) {
        ProviderPanel(access, recover, refresh)
        return
    }
    if (live.accessRevision != access.revision) {
        CircularProgressIndicator(Modifier.testTag("records-loading"))
        return
    }
    when (val result = live.state) {
        RecordsState.Loading -> CircularProgressIndicator(Modifier.testTag("records-loading"))
        is RecordsState.AccessRequired -> {
            StateMessage(R.string.access_required, request, R.string.request_access)
            TextButton(settings, Modifier.testTag("missing-access-settings")) { Text(stringResource(R.string.health_settings)) }
        }
        is RecordsState.ProviderUnavailable -> StateMessage(R.string.provider_unavailable, recover, R.string.provider_recover, refresh)
        is RecordsState.UnsupportedFeature -> StateMessage(if (result.feature == HealthFeature.HISTORY) R.string.history_unsupported else R.string.feature_unsupported, recover, R.string.provider_recover, refresh)
        is RecordsState.NotImplemented -> NoteCard(stringResource(R.string.reader_planned), stringResource(R.string.reader_planned_body))
        is RecordsState.Failed -> StateMessage(if (result.reason == ReadFailure.ACCESS_CHANGED) R.string.access_changed else R.string.read_failed, refresh, R.string.retry)
        RecordsState.HistoryAccessRequired -> StateMessage(R.string.history_limited, history, R.string.history_request)
        is RecordsState.Content, is RecordsState.Empty -> {
            if (!access.environment!!.historyGranted) HistoryPanel(access.environment!!, history)
            live.recentWindow?.let { NoteCard(stringResource(R.string.recent_window), stringResource(R.string.recent_window_body, it.start.toString(), it.end.toString())) }
            if (live.records.isEmpty() && result is RecordsState.Empty) {
                NoteCard(stringResource(R.string.no_records), stringResource(R.string.no_records_body))
                TextButton(chooseDate) { Text(stringResource(R.string.another_date)) }
            } else {
                Text(pluralStringResource(if ((result as? RecordsState.Content)?.page?.next != null) R.plurals.loaded_count else R.plurals.complete_record_count, live.records.size, live.records.size))
                if (showRows) live.records.forEach { record ->
                    CatalogRow(recordValue(record), recordTime(record) + "\n" + record.metadata.originPackage, "record-${record.metadata.id}") { openRecord(record.metadata.id) }
                }
                if (showRows && (result as? RecordsState.Content)?.page?.next != null) {
                    Button(loadMore, enabled = !live.loadingMore, modifier = Modifier.testTag("load-more")) { Text(stringResource(R.string.load_more)) }
                }
            }
            if (showRows) TextButton(retry) { Text(stringResource(R.string.refresh_records)) }
        }
    }
}

@Composable
private fun StateMessage(message: Int, action: () -> Unit, label: Int, retry: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(message), Modifier.testTag("access-message"))
        Button(action) { Text(stringResource(label)) }
        retry?.let { TextButton(it) { Text(stringResource(R.string.retry)) } }
    }
}

/** Each raw record is a lazy item, including when hundreds of pages are loaded. */
fun androidx.compose.foundation.lazy.LazyListScope.liveRecordsItems(type: RecordType, access: AccessUiState, live: LiveRecordsUiState,
    request: () -> Unit, history: () -> Unit, recover: () -> Unit, settings: () -> Unit, refresh: () -> Unit,
    retry: () -> Unit, loadMore: () -> Unit, chooseDate: () -> Unit, openRecord: (String) -> Unit, showHero: Boolean = true) {
    val historyMetric = type.id in listOf("vo2_max", "resting_heart_rate")
    val showHistory = historyMetric && access.foreground && access.environment?.provider == ProviderStatus.AVAILABLE &&
        live.accessRevision == access.revision && live.state is RecordsState.Content
    if (showHistory) {
        val points = live.records.mapNotNull { record ->
            val value = record.fields.firstOrNull { it.key == if (type.id == "vo2_max") "vo2MillilitersPerMinuteKilogram" else "beatsPerMinute" }?.value
            val number = when (value) { is HealthValue.Integer -> value.value.toDouble(); is HealthValue.Decimal -> value.value; else -> null }
            number?.let { ChartPoint(record.startTime(), it) }
        }
        item { RecordedChart(points, if (type.id == "vo2_max") "mL/(min·kg)" else "bpm") }
        item { HistoryRecordTable(live.records, openRecord, includeTime = type.id == "resting_heart_rate") }
    }
    if (showHero && !historyMetric && access.foreground && access.environment?.provider == ProviderStatus.AVAILABLE &&
        live.accessRevision == access.revision && live.state is RecordsState.Content) {
        live.records.maxByOrNull { it.startTime() }?.let { record -> item { RecordMetricHero(record, latest = true) } }
    }
    item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LiveRecordsPanel(type, access, live, request, history, recover, settings, refresh, retry, loadMore, chooseDate, openRecord, showRows = false)
    } }
    if (access.foreground && access.environment?.provider == ProviderStatus.AVAILABLE && live.accessRevision == access.revision &&
        (live.state is RecordsState.Content || live.state is RecordsState.Empty)) {
        if (!showHistory) item { SectionTitle(stringResource(R.string.raw_records)) }
        if (type.id == "steps") item { Text(stringResource(R.string.raw_steps_note), style = MaterialTheme.typography.bodySmall) }
        if (!showHistory) items(live.records, key = { it.metadata.id }) { record ->
            CatalogRow(recordValue(record), recordTime(record) + "\n" + record.metadata.originPackage, "record-${record.metadata.id}") { openRecord(record.metadata.id) }
        }
        if ((live.state as? RecordsState.Content)?.page?.next != null) item {
            Button(loadMore, enabled = !live.loadingMore, modifier = Modifier.testTag("load-more")) { Text(stringResource(R.string.load_more)) }
        }
        item { TextButton(retry) { Text(stringResource(R.string.refresh_records)) } }
    }
}
