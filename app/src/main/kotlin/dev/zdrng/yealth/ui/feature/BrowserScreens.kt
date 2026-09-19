package dev.zdrng.yealth.ui.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import dev.zdrng.yealth.R
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.ui.components.*
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TodayScreen(content: BrowserContent, state: BrowserUiState, chooseDate: () -> Unit, moveDate: (Long) -> Unit, openMetric: (String) -> Unit, openAccess: () -> Unit, stepsSummary: (@Composable () -> Unit)? = null, openActivity: () -> Unit = {},
    otherSummary: (@Composable (String) -> Unit)? = null) {
    ScreenList("screen-today") {
        item { TodayHeader(state.date, chooseDate) }
        item { HealthConnectIntro() }
        items(listOf("steps", "sleep_session", "heart_rate"), key = { it }) { id ->
            if (id == "steps" && stepsSummary != null) stepsSummary()
            else if (id != "steps" && otherSummary != null) otherSummary(id)
            else {
            val record = state.records.firstOrNull { it.typeId == id }
            DataCard(stringResource(if (id == "sleep_session") R.string.summary_sleep else typeLabels.getValue(id)),
                record?.let { summaryValue(it) } ?: stringResource(if (content.isPreview) R.string.no_records else R.string.open_records),
                record?.let(::summarySupporting) ?: "",
                { openMetric(id) }, Modifier.testTag("summary-$id"), tone = listOf("steps", "sleep_session", "heart_rate").indexOf(id),
                category = metricCategory(id), typeId = id)
            }
        }
        if (!content.isPreview) item { TextButton(openActivity, Modifier.testTag("open-activity")) { Text(stringResource(R.string.open_activity)) } }
        if (!content.isPreview) item { TextButton(openAccess) { Text(stringResource(R.string.about_access)) } }
        if (content.isPreview) item {
            Text(stringResource(R.string.sample_banner), Modifier.fillMaxWidth().padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun BrowseScreen(content: BrowserContent, state: BrowserUiState, search: (String) -> Unit, openCategory: (HealthCategory) -> Unit, openMetric: (String) -> Unit) {
    val resources = LocalResources.current
    val labels = typeLabels.mapValues { resources.getString(it.value) }
    val categories = HealthCategory.entries.associateWith { resources.getString(categoryLabel(it)) }
    val categoryOrder = listOf(HealthCategory.ACTIVITY, HealthCategory.BODY_MEASUREMENTS, HealthCategory.SLEEP,
        HealthCategory.NUTRITION, HealthCategory.VITALS, HealthCategory.WELLNESS, HealthCategory.CYCLE_TRACKING)
    val filtered = filterCatalog(content.catalog, state.query, labels, categories)
    val keyboard = LocalSoftwareKeyboardController.current
    ScreenList("screen-browse") {
        item { Heading(stringResource(R.string.browse_records).replaceFirst(" ", "\n"), artwork = HeadingArtwork.Search) }
        item {
            TextField(value = state.query, onValueChange = search, modifier = Modifier.fillMaxWidth().testTag("catalog-search"),
                placeholder = { Text(stringResource(R.string.search_health)) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (state.query.isNotEmpty()) IconButton({ search("") }) { Icon(Icons.Default.Close, stringResource(R.string.clear_search)) } },
                singleLine = true, shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }))
        }
        if (state.query.isBlank()) {
            // Two columns where text fits; a single column at large font scales or narrow widths.
            item {
                BoxWithConstraints {
                    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.15f
                    val columns = if (maxWidth >= 360.dp && !largeText) 2 else 1
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        categoryOrder.chunked(columns).forEach { group ->
                            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                group.forEach { category ->
                                    CategoryTile(categories.getValue(category), pluralStringResource(R.plurals.type_count, content.catalog.count { it.category == category }, content.catalog.count { it.category == category }),
                                        { openCategory(category) }, Modifier.weight(1f).fillMaxHeight().testTag("category-${category.name}"), category.ordinal, category)
                                }
                                if (group.size < columns) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            if (filtered.isEmpty()) item { NoteCard(stringResource(R.string.no_matches), stringResource(R.string.search_hint)) }
            items(filtered, key = { it.id }) { type ->
                CatalogRow(labels.getValue(type.id), categories.getValue(type.category), "type-${type.id}") { keyboard?.hide(); openMetric(type.id) }
            }
        }
    }
}

@Composable
fun CategoryScreen(category: HealthCategory, content: BrowserContent, state: BrowserUiState,
    chooseDate: () -> Unit, moveDate: (Long) -> Unit, selectPeriod: (Period) -> Unit, openMetric: (String) -> Unit, stepsSummary: (@Composable () -> Unit)? = null,
    typeContent: (@Composable (String) -> Unit)? = null, categoryEmpty: Boolean = false) {
    val preferred = listOf("steps", "exercise_session", "distance", "vo2_max", "resting_heart_rate")
    val types = content.catalog.filter { it.category == category }.sortedBy { preferred.indexOf(it.id).let { n -> if (n < 0) 100 else n } }
    val categoryRecords = state.records.filter { record -> types.any { it.id == record.typeId } }
    val emptyCategory = (content.isPreview && categoryRecords.isEmpty()) || (!content.isPreview && categoryEmpty)
    ScreenList("screen-category") {
        item { Heading(stringResource(categoryLabel(category)), category,
            decorate = !(category == HealthCategory.NUTRITION && (categoryRecords.isEmpty() || categoryEmpty))) }
        if (category == HealthCategory.ACTIVITY) item { PeriodPicker(state.period, false, selectPeriod) }
        item { DateControls(state.date, { moveDate(-1) }, { moveDate(1) }, chooseDate) }
        if (state.period != Period.DAY) item { RangeLabel(state) }
        if (content.isPreview && categoryRecords.isEmpty()) item { EmptyRecords(chooseDate, category = category) }
        if (!content.isPreview && categoryEmpty) item { EmptyRecords(chooseDate, false, category) }
        if (!emptyCategory) items(types, key = { it.id }) { type ->
            if (type.id == "steps" && stepsSummary != null) stepsSummary()
            else if (typeContent != null) typeContent(type.id)
            else {
            val record = categoryRecords.firstOrNull { it.typeId == type.id }
            DataCard(stringResource(typeLabels.getValue(type.id)), record?.let { summaryValue(it) } ?: "",
                if (record == null) stringResource(if (content.isPreview) R.string.no_records else R.string.open_records)
                else summarySupporting(record),
                { openMetric(type.id) }, Modifier.testTag("type-${type.id}"), types.indexOf(type), category, typeId = type.id)
            }
        }
    }
}

@Composable
fun MetricScreen(typeId: String, content: BrowserContent, state: BrowserUiState, chooseDate: () -> Unit, moveDate: (Long) -> Unit,
    selectPeriod: (Period) -> Unit, openRecord: (String) -> Unit, openAccess: () -> Unit, livePanel: (androidx.compose.foundation.lazy.LazyListScope.() -> Unit)? = null) {
    val records = state.records.filter { it.typeId == typeId }
    val history = typeId in listOf("vo2_max", "resting_heart_rate")
    ScreenList("screen-metric") {
        item { Heading(stringResource(typeLabels.getValue(typeId)), metricCategory(typeId), decorate = !history) }
        item { PeriodPicker(state.period, history, selectPeriod,
            selectionColor = if (typeId == "resting_heart_rate") dev.zdrng.yealth.ui.theme.LocalMockupColors.current.lemon else null) }
        if (!history) item { DateControls(state.date, { moveDate(-1) }, { moveDate(1) }, chooseDate) }
        if (!history) item { RangeLabel(state) }
        if (livePanel != null) livePanel()
        else if (!content.isPreview) {
            item { NoteCard(stringResource(R.string.not_connected), stringResource(R.string.not_connected_body)) }
            item { Button(openAccess) { Text(stringResource(R.string.about_access)) } }
        } else if (records.isEmpty()) item { EmptyRecords(chooseDate, category = metricCategory(typeId)) }
        else {
            if (typeId == "steps" && state.period == Period.WEEK) item {
                val formatter = DateTimeFormatter.ofPattern("EEEEE").withZone(ZoneId.systemDefault())
                val bars = records.sortedBy { it.startTime() }.mapNotNull { record ->
                    val count = (record.fields.firstOrNull { it.key == "count" }?.value as? HealthValue.Integer)?.value
                    count?.let { RecordedBar(formatter.format(record.startTime()), it.toDouble(), recordTime(record) + ": " + recordValue(record)) }
                }
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecordMetricHero(records.maxBy { it.startTime() }, latest = true)
                    RoundedBarChart(bars, stringResource(R.string.steps_unit), Modifier.testTag("sample-steps-chart"))
                    Text(stringResource(R.string.sample_bar_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!history && (typeId != "steps" || state.period != Period.WEEK)) item {
                RecordMetricHero(records.maxBy { it.startTime() }, latest = true)
            }
            if (history) item {
                val points = records.mapNotNull { record ->
                    val number = record.fields.firstOrNull()?.value
                    val value = when (number) { is HealthValue.Integer -> number.value.toDouble(); is HealthValue.Decimal -> number.value; else -> null }
                    value?.let { ChartPoint(record.startTime(), it) }
                }
                RecordedChart(points, if (typeId == "vo2_max") "mL/(min·kg)" else "bpm")
            }
            if (history) {
                item { HistoryRecordTable(records, openRecord, includeTime = typeId == "resting_heart_rate") }
                item { Text(pluralStringResource(R.plurals.sample_count, records.size, records.size),
                    Modifier.fillMaxWidth().padding(top = 4.dp), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                item { SectionTitle(stringResource(R.string.raw_records)) }
                item { Text(pluralStringResource(R.plurals.sample_count, records.size, records.size), style = MaterialTheme.typography.bodyMedium) }
                items(records, key = { it.metadata.id }) { record ->
                    CatalogRow(recordValue(record), recordTime(record) + "\n" + record.metadata.originPackage, "record-${record.metadata.id}") { openRecord(record.metadata.id) }
                }
            }
            item { Text(stringResource(R.string.values_note), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun RecordScreen(typeId: String, record: HealthRecord?, openAccess: () -> Unit,
    routeContent: (@Composable () -> Unit)? = null) {
    ScreenList("screen-record") {
        if (record == null) {
            item { NoteCard(stringResource(R.string.record_unavailable), stringResource(R.string.record_unavailable_body)) }
        } else {
            val sourceSummary = record.metadata.device?.let { listOfNotNull(it.manufacturer, it.model).joinToString(" · ").ifBlank { null } }
                ?: record.metadata.originPackage
            val zoneSummary = when (val time = record.time) {
                is RecordTime.Point -> time.offset
                is RecordTime.Interval -> time.startOffset
            }?.toString() ?: "Not provided"
            item { Heading(stringResource(typeLabels.getValue(typeId)), metricCategory(typeId)) }
            item { RecordMetricHero(record) }
            item { HighlightValueRow(stringResource(R.string.recorded_at), recordClockTime(record), Icons.Default.DateRange, 0) }
            item { HighlightValueRow(stringResource(R.string.source), sourceSummary, Icons.Default.Settings, 1) }
            item { HighlightValueRow("Zone", zoneSummary, Icons.Default.Place, 2) }
            item { HighlightValueRow(stringResource(R.string.metadata),
                stringResource(R.string.recording_method) + " · " + record.metadata.recordingMethod, Icons.Default.Info, 3) }
            item { Button(openAccess, Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = dev.zdrng.yealth.ui.theme.Electric,
                    contentColor = androidx.compose.ui.graphics.Color.White)) { Text(stringResource(R.string.health_settings)) } }
            item { SectionTitle(stringResource(R.string.record_details)) }
            when (val time = record.time) {
                is RecordTime.Point -> {
                    item { ValueRow(stringResource(R.string.recorded_at), time.time.toString()) }
                    item { ValueRow(stringResource(R.string.utc_offset), time.offset?.toString() ?: stringResource(R.string.not_provided)) }
                }
                is RecordTime.Interval -> {
                    item { ValueRow(stringResource(R.string.starts_at), time.start.toString()) }
                    item { ValueRow(stringResource(R.string.ends_at), time.end.toString()) }
                    item { ValueRow(stringResource(R.string.start_offset), time.startOffset?.toString() ?: stringResource(R.string.not_provided)) }
                    item { ValueRow(stringResource(R.string.end_offset), time.endOffset?.toString() ?: stringResource(R.string.not_provided)) }
                }
            }
            item { ValueRow(stringResource(R.string.source), record.metadata.originPackage) }
            item { ValueRow(stringResource(R.string.device), record.metadata.device?.let { listOfNotNull(it.manufacturer, it.model).joinToString(" · ").ifBlank { null } } ?: stringResource(R.string.not_provided)) }
            item { SectionTitle(stringResource(R.string.fields)) }
            if (record.fields.isEmpty() && record.samples.isEmpty()) item { Text(stringResource(R.string.fields_unavailable)) }
            items(flattenedFields(record.fields), key = { it.key }) { field -> ValueRow(fieldName(field.key), formatValue(field.value)) }
            if (routeContent != null) item { routeContent() }
            if (record.samples.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.samples)) }
                items(record.samples) { sample ->
                    ValueRow(sample.time.toString(), sample.fields.joinToString("\n") { it.key + ": " + plainValue(it.value) })
                }
            }
            item { SectionTitle(stringResource(R.string.metadata)) }
            item { ValueRow(stringResource(R.string.record_id), record.metadata.id) }
            item { ValueRow(stringResource(R.string.last_modified), record.metadata.lastModified.toString()) }
            item { ValueRow(stringResource(R.string.client_id), record.metadata.clientRecordId ?: stringResource(R.string.not_provided)) }
            item { ValueRow(stringResource(R.string.client_version), record.metadata.clientRecordVersion.toString()) }
            item { ValueRow(stringResource(R.string.recording_method), record.metadata.recordingMethod.toString()) }
            record.metadata.device?.let { device -> item { ValueRow(stringResource(R.string.device_type), device.type.toString()) } }
            item { Text(stringResource(R.string.values_note), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun AccessScreen(content: BrowserContent, openCategoryAccess: (HealthCategory) -> Unit, openPrivacy: () -> Unit) {
    val featured = listOf(HealthCategory.ACTIVITY, HealthCategory.SLEEP, HealthCategory.VITALS, HealthCategory.NUTRITION)
    val remaining = HealthCategory.entries.filterNot(featured::contains)
    ScreenList("screen-access") {
        item { Heading(stringResource(R.string.access), artwork = HeadingArtwork.Access) }
        item {
            Column(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.access_summary), style = MaterialTheme.typography.titleLargeEmphasized)
                Text(stringResource(R.string.access_summary_body), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (content.isPreview) Text(stringResource(R.string.sample_banner), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(featured, key = { it.name }) { category ->
            val status = if (!content.isPreview) R.string.access_not_requested else when (category) {
                HealthCategory.ACTIVITY, HealthCategory.SLEEP -> R.string.shared
                HealthCategory.VITALS -> R.string.partial_shared
                else -> R.string.not_shared
            }
            AccessRow(category, stringResource(categoryLabel(category)), stringResource(status), "access-${category.name}") { openCategoryAccess(category) }
        }
        item { Button(openPrivacy, Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.extraLarge) {
            Text(stringResource(R.string.about_access))
        } }
        item { Text(stringResource(R.string.offline_note), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(remaining, key = { it.name }) { category ->
            val status = if (!content.isPreview) R.string.access_not_requested else R.string.not_shared
            AccessRow(category, stringResource(categoryLabel(category)), stringResource(status), "access-${category.name}") { openCategoryAccess(category) }
        }
    }
}

@Composable
fun AccessDialog(category: HealthCategory?, content: BrowserContent, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (category == null) stringResource(R.string.privacy) else stringResource(R.string.access_dialog, stringResource(categoryLabel(category)))) },
        text = { Text(if (category == null) stringResource(R.string.privacy_body) + "\n\n" + stringResource(R.string.not_connected_body)
            else pluralStringResource(R.plurals.access_explanation, content.catalog.count { it.category == category }, content.catalog.count { it.category == category })) },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.close)) } })
}

@Composable
private fun EmptyRecords(chooseDate: () -> Unit, preview: Boolean = true, category: HealthCategory? = null) {
    if (category == HealthCategory.NUTRITION) {
        Column(Modifier.fillMaxWidth().testTag("empty-records").padding(horizontal = 18.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            EmptyNutritionArtwork(Modifier.fillMaxWidth().height(190.dp))
            Text(stringResource(R.string.no_records_day), style = MaterialTheme.typography.displaySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(stringResource(if (preview) R.string.no_samples else R.string.no_records_body), style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            OutlinedButton(chooseDate, Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                Icon(Icons.Default.DateRange, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.another_date))
            }
        }
        return
    }
    Card(modifier = Modifier.fillMaxWidth().testTag("empty-records"), shape = MaterialTheme.shapes.extraLarge,
        colors = if (category != null) categoryCardColors(category)
            else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            category?.let { HealthIcon(it, prominent = true) }
            Text(stringResource(R.string.no_records), style = MaterialTheme.typography.headlineLargeEmphasized)
            Text(stringResource(if (preview) R.string.no_samples else R.string.no_records_body), style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            OutlinedButton(chooseDate) { Text(stringResource(R.string.another_date)) }
        }
    }
}

@Composable
private fun AccessRow(category: HealthCategory, title: String, supporting: String, tag: String, onClick: () -> Unit) {
    val colors = when (category) {
        HealthCategory.ACTIVITY, HealthCategory.NUTRITION -> CardDefaults.cardColors(dev.zdrng.yealth.ui.theme.LocalMockupColors.current.lilac, dev.zdrng.yealth.ui.theme.Ink)
        HealthCategory.SLEEP -> CardDefaults.cardColors(dev.zdrng.yealth.ui.theme.LocalMockupColors.current.mint, dev.zdrng.yealth.ui.theme.Ink)
        HealthCategory.VITALS -> CardDefaults.cardColors(dev.zdrng.yealth.ui.theme.LocalMockupColors.current.lemon, dev.zdrng.yealth.ui.theme.Ink)
        else -> categoryCardColors(category)
    }
    Card(onClick, Modifier.fillMaxWidth().testTag(tag), shape = MaterialTheme.shapes.extraLarge,
        colors = colors) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HealthIcon(category)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                Text(supporting, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

@Composable
private fun HighlightValueRow(label: String, value: String, icon: ImageVector, tone: Int) {
    val colors = when (tone) {
        0 -> CardDefaults.cardColors(dev.zdrng.yealth.ui.theme.LocalMockupColors.current.lemon, dev.zdrng.yealth.ui.theme.Ink)
        2 -> CardDefaults.cardColors(dev.zdrng.yealth.ui.theme.LocalMockupColors.current.mint, dev.zdrng.yealth.ui.theme.Ink)
        else -> CardDefaults.cardColors(dev.zdrng.yealth.ui.theme.LocalMockupColors.current.lilac, dev.zdrng.yealth.ui.theme.Ink)
    }
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = colors) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape,
                color = dev.zdrng.yealth.ui.theme.Ink.copy(alpha = .10f), contentColor = dev.zdrng.yealth.ui.theme.Ink) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(value, style = MaterialTheme.typography.titleMediumEmphasized)
            }
        }
    }
}

@Composable
internal fun HistoryRecordTable(records: List<HealthRecord>, openRecord: (String) -> Unit, includeTime: Boolean = false) {
    val labelStyle = MaterialTheme.typography.labelSmall
    val valueStyle = MaterialTheme.typography.bodySmall
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(Modifier.fillMaxWidth().testTag("history-record-table"), shape = MaterialTheme.shapes.extraLarge,
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Date", Modifier.weight(.75f), style = labelStyle, color = muted)
            Text("Value", Modifier.weight(1.6f), style = labelStyle, color = muted)
            if (includeTime) Text("Time", Modifier.weight(.65f), style = labelStyle, color = muted)
            Text("Source", Modifier.weight(1.1f), style = labelStyle, color = muted)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        records.sortedByDescending { it.startTime() }.forEach { record ->
            val offset = when (val time = record.time) {
                is RecordTime.Point -> time.offset
                is RecordTime.Interval -> time.startOffset
            } ?: ZoneOffset.UTC
            val date = DateTimeFormatter.ofPattern("d MMM").withZone(offset).format(record.startTime())
            val fullDate = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(offset).format(record.startTime())
            val time = DateTimeFormatter.ofPattern("HH:mm").withZone(offset).format(record.startTime())
            val source = record.metadata.device?.let { listOfNotNull(it.manufacturer, it.model).joinToString(" · ").ifBlank { null } }
                ?: record.metadata.originPackage
            Row(Modifier.fillMaxWidth().heightIn(min = 42.dp).clickable { openRecord(record.metadata.id) }
                .testTag("record-${record.metadata.id}").padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(date, Modifier.weight(.75f).semantics { contentDescription = fullDate }, style = valueStyle, maxLines = 1)
                Text(recordValue(record), Modifier.weight(1.6f), style = MaterialTheme.typography.titleSmall)
                if (includeTime) Text(time, Modifier.weight(.65f), style = valueStyle, maxLines = 1)
                Text(source, Modifier.weight(1.1f), style = labelStyle, color = muted, maxLines = 2)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
    }
}

@Composable
internal fun CatalogRow(title: String, supporting: String, tag: String, onClick: () -> Unit) {
    Card(onClick, Modifier.fillMaxWidth().testTag(tag), shape = MaterialTheme.shapes.large) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.titleMediumEmphasized) },
            supportingContent = { Text(supporting) }, trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))
    }
}

@Composable
private fun TodayHeader(date: java.time.LocalDate, chooseDate: () -> Unit) {
    val visibleDate = date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    val fullDate = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    val chooseDateDescription = stringResource(R.string.choose_date) + ": " + fullDate
    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    @Composable fun DatePill(modifier: Modifier) {
        FilledTonalButton(chooseDate,
            modifier.then(if (largeText) Modifier.fillMaxWidth() else Modifier.width(158.dp)).heightIn(min = 42.dp).testTag("choose-date").semantics { contentDescription = chooseDateDescription },
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            Icon(Icons.Default.DateRange, null, Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(visibleDate, maxLines = 1)
        }
    }
    if (largeText) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Heading(stringResource(R.string.today))
        DatePill(Modifier)
    } else Box(Modifier.fillMaxWidth().height(132.dp)) {
        Heading(stringResource(R.string.today))
        DatePill(Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun HealthConnectIntro() {
    Card(Modifier.fillMaxWidth().heightIn(min = 170.dp), shape = OrganicShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.health_connect),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Black))
                Text(stringResource(R.string.health_intro), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.offline_note), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .76f))
            }
            HealthConnectArtwork(Modifier.size(96.dp))
        }
    }
}

@Composable
private fun RangeLabel(state: BrowserUiState) {
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    Text(stringResource(R.string.selected_range, state.period.start(state.date).format(format), state.date.format(format)), style = MaterialTheme.typography.labelLarge)
}

@Composable
internal fun recordValue(record: HealthRecord): String {
    fun field(key: String) = record.fields.firstOrNull { it.key == key && it.value != HealthValue.Missing &&
        (it.value !is HealthValue.Text || it.value.value.isNotBlank()) }
    if (record.typeId.endsWith("_session")) {
        field("title")?.let { return formatValue(it.value) }
        (record.time as? RecordTime.Interval)?.let {
            val minutes = Duration.between(it.start, it.end).toMinutes()
            return if (minutes >= 60) stringResource(R.string.session_hours_minutes, minutes / 60, minutes % 60)
                else stringResource(R.string.session_minutes, minutes)
        }
    }
    if (record.typeId == "blood_pressure") {
        val systolic = field("systolic")?.value
        val diastolic = field("diastolic")?.value
        if (systolic != null && diastolic != null) return formatValue(systolic) + " / " + formatValue(diastolic)
    }
    val preferred = when (record.typeId) {
        "nutrition" -> listOf("name", "energy", "protein", "totalCarbohydrate", "totalFat")
        "skin_temperature" -> listOf("baseline")
        else -> listOf("count", "beatsPerMinute", "vo2MillilitersPerMinuteKilogram", "distance", "energy", "mass", "weight",
            "height", "percentage", "temperature", "level", "volume", "rate", "power", "speed", "floors", "elevation", "basalMetabolicRate")
    }
    preferred.firstNotNullOfOrNull(::field)?.let { return formatValue(it.value) }
    record.samples.maxByOrNull { it.time }?.fields?.firstOrNull { it.value != HealthValue.Missing }?.let { return formatValue(it.value) }
    record.fields.firstOrNull { it.value is HealthValue.Integer || it.value is HealthValue.Decimal || it.value is HealthValue.Text }?.let {
        return fieldName(it.key) + ": " + formatValue(it.value)
    }
    record.fields.firstOrNull { it.value is HealthValue.Code }?.let { return fieldName(it.key) + ": " + formatValue(it.value) }
    return stringResource(R.string.record_value_unavailable)
}

@Composable
private fun summaryValue(record: HealthRecord): String {
    if (record.typeId == "steps") {
        val count = (record.fields.firstOrNull { it.key == "count" }?.value as? HealthValue.Integer)?.value
        if (count != null) return stringResource(R.string.steps_total_value, java.text.NumberFormat.getIntegerInstance().format(count))
    }
    if (record.typeId == "distance") {
        val distance = (record.fields.firstOrNull { it.key == "distance" }?.value as? HealthValue.Decimal)?.value
        if (distance != null) return if (distance >= 1000) "%.1f km".format(distance / 1000.0) else "%.0f m".format(distance)
    }
    if (record.typeId == "sleep_session") return recordValue(record).removeSuffix(" · session")
    return recordValue(record)
}

private fun summarySupporting(record: HealthRecord): String {
    val source = record.metadata.device?.let { listOfNotNull(it.manufacturer, it.model).joinToString(" · ").ifBlank { null } }
        ?: record.metadata.originPackage.substringAfterLast('.')
    return "$source · ${compactRecordTime(record)}"
}

private fun recordClockTime(record: HealthRecord): String {
    val offset = when (val time = record.time) {
        is RecordTime.Point -> time.offset
        is RecordTime.Interval -> time.startOffset
    } ?: ZoneOffset.UTC
    return DateTimeFormatter.ofPattern("h:mm a").withZone(offset).format(record.startTime())
}

internal fun recordTime(record: HealthRecord): String {
    val offset = when (val t = record.time) { is RecordTime.Point -> t.offset; is RecordTime.Interval -> t.startOffset } ?: ZoneOffset.UTC
    return DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm XXX").withZone(offset).format(record.startTime())
}

private fun compactRecordTime(record: HealthRecord): String {
    val offset = when (val time = record.time) { is RecordTime.Point -> time.offset; is RecordTime.Interval -> time.startOffset } ?: ZoneOffset.UTC
    return DateTimeFormatter.ofPattern("HH:mm XXX").withZone(offset).format(record.startTime())
}

private fun fieldName(key: String): String = key.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }

@Composable
private fun formatValue(value: HealthValue): String = when (value) {
    HealthValue.Missing -> stringResource(R.string.not_provided)
    is HealthValue.Code -> stringResource(R.string.unknown_code, value.value)
    is HealthValue.Flag -> stringResource(if (value.value) R.string.yes else R.string.no)
    else -> plainValue(value)
}

private fun plainValue(value: HealthValue): String = when (value) {
    is HealthValue.Integer -> listOfNotNull(value.value.toString(), value.unit).joinToString(" ")
    is HealthValue.Decimal -> "${value.value} ${value.unit}"
    is HealthValue.Text -> value.value
    is HealthValue.Code -> value.value.toString()
    is HealthValue.Flag -> value.value.toString()
    is HealthValue.Timestamp -> value.value.toString()
    is HealthValue.Offset -> value.value.toString()
    is HealthValue.Fields -> value.fields.joinToString("\n") { fieldName(it.key) + ": " + plainValue(it.value) }
    is HealthValue.Items -> value.values.joinToString("\n") { plainValue(it) }
    HealthValue.Missing -> "—"
}
