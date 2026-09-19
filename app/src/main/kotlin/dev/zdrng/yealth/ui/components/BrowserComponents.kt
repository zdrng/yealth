@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.zdrng.yealth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import dev.zdrng.yealth.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import dev.zdrng.yealth.R
import dev.zdrng.yealth.domain.model.HealthCategory
import dev.zdrng.yealth.ui.feature.Period
import dev.zdrng.yealth.ui.feature.periodLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ScreenList(tag: String, content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().testTag(tag),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
fun Heading(text: String, category: HealthCategory? = null, decorate: Boolean = true, artwork: HeadingArtwork = HeadingArtwork.Abstract) {
    Box(Modifier.fillMaxWidth().heightIn(min = if (!decorate) 72.dp else if (text.contains("\n")) 124.dp else 98.dp)) {
        if (decorate) HeadingBloom(artwork, Modifier.align(Alignment.CenterEnd).size(112.dp)) {
            category?.let { CompositionLocalProvider(LocalContentColor provides Ink) { HealthIcon(it, prominent = true) } }
        }
        Text(text, style = MaterialTheme.typography.displayLarge,
            fontSize = if (text.length >= 18) 38.sp else if (text.length > 10) 54.sp else 64.sp,
            lineHeight = if (text.length >= 18) 42.sp else if (text.length > 10) 51.sp else 68.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp)
                .fillMaxWidth(if (decorate) .76f else 1f).align(Alignment.CenterStart).semantics { heading() })
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMediumEmphasized,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp).semantics { heading() })
}

@Composable
fun NoteCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun DataCard(title: String, value: String, supporting: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: Int = 0,
    category: HealthCategory? = null, typeId: String? = null) {
    val palette = LocalMockupColors.current
    val colors = if (category == HealthCategory.ACTIVITY) {
        CardDefaults.cardColors(when { typeId == "distance" || (typeId == null && tone == 2) -> palette.mint; typeId == "exercise_session" || (typeId == null && tone == 1) -> palette.lemon; else -> palette.lilac }, Ink)
    } else category?.let { categoryCardColors(it) } ?: CardDefaults.cardColors()
    Card(onClick, modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            category?.let { HealthIcon(it, prominent = true, typeId = typeId ?: if (category == HealthCategory.ACTIVITY && tone == 2) "distance" else null) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                if (value.isNotEmpty()) SummaryValue(value)
                if (supporting.isNotEmpty()) Text(supporting, style = MaterialTheme.typography.labelMedium)
            }
            ChevronBadge()
        }
    }
}

@Composable
fun CategoryTile(title: String, count: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: Int = 0,
    category: HealthCategory? = null) {
    val palette = LocalMockupColors.current
    val colors = when (category) {
        HealthCategory.ACTIVITY -> CardDefaults.cardColors(palette.lemon, Ink)
        HealthCategory.VITALS -> CardDefaults.cardColors(palette.lilac, Ink)
        else -> category?.let { categoryCardColors(it) } ?: CardDefaults.cardColors()
    }
    Card(onClick, modifier.heightIn(min = 148.dp), shape = MaterialTheme.shapes.large, colors = colors) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                category?.let { HealthIcon(it, backgroundColor = when (it) { HealthCategory.ACTIVITY -> Color(0xFFEBD14C).copy(alpha = .4f); HealthCategory.VITALS -> Color(0xFF8A58DC).copy(alpha = .18f); else -> null }) }
                ChevronBadge()
            }
            Text(if (category == HealthCategory.BODY_MEASUREMENTS) title.replaceFirst(" ", "\n") else title, style = MaterialTheme.typography.titleMediumEmphasized)
            Spacer(Modifier.weight(1f))
            Text(count, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun DateControls(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onChoose: () -> Unit) {
    val dateLabel = stringResource(R.string.choose_date) + ": " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilledTonalIconButton(onPrevious, Modifier.size(46.dp).testTag("previous-date"), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_date))
        }
        FilledTonalButton(onChoose, Modifier.weight(1f).padding(horizontal = 24.dp).testTag("choose-date").semantics { contentDescription = dateLabel }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
            Icon(Icons.Default.DateRange, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(date.format(DateTimeFormatter.ofPattern("d MMM")))
        }
        FilledTonalIconButton(onNext, Modifier.size(46.dp).testTag("next-date"), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_date))
        }
    }
}

@Composable
fun PeriodPicker(period: Period, history: Boolean, onSelect: (Period) -> Unit, selectionColor: Color? = null) {
    val options = if (history) listOf(Period.DAYS_7, Period.DAYS_30, Period.DAYS_90, Period.YEAR) else listOf(Period.DAY, Period.WEEK, Period.MONTH)
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
        .padding(3.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { option ->
            val checked = period == option
            Box(Modifier.weight(1f).height(44.dp)
                .background(if (checked) selectionColor ?: MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                .clickable { onSelect(option) }.testTag("period-${option.name}")
                .semantics { role = Role.RadioButton; selected = checked }, contentAlignment = Alignment.Center) {
                Text(stringResource(periodLabel(option)), color = if (checked) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun DateDialog(date: LocalDate, onDismiss: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = {
            state.selectedDateMillis?.let { onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            onDismiss()
        }, enabled = state.selectedDateMillis != null) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } }) {
        DatePicker(state)
    }
}

@Composable
fun ValueRow(label: String, value: String) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        ListItem(headlineContent = { Text(value, style = MaterialTheme.typography.bodyLarge) },
            overlineContent = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))
    }
}

data class ChartPoint(val instant: Instant, val value: Double)

/** No inferred trend or aggregate: a dot per original value, positioned by actual timestamp. */
@Composable
fun RecordedChart(points: List<ChartPoint>, unit: String) {
    if (points.isEmpty()) return
    val palette = LocalMockupColors.current
    val sorted = points.sortedBy { it.instant }
    val low = sorted.minOf { it.value }
    val high = sorted.maxOf { it.value }
    val isHeartRate = unit == "bpm"
    val tickStep = if (isHeartRate) 5.0 else 2.0
    val bottom = kotlin.math.floor((low - tickStep * .5) / tickStep) * tickStep
    val top = kotlin.math.ceil((high + tickStep * .5) / tickStep) * tickStep
    val color = if (isHeartRate) Color(0xFFBE9200) else Lilac
    val chartText = if (isHeartRate) Ink else Color.White
    val grid = chartText.copy(alpha = .3f)
    val description = stringResource(R.string.chart_description)
    val formatter = DateTimeFormatter.ofPattern("d MMM").withZone(java.time.ZoneId.systemDefault())
    Card(shape = OrganicShape, colors = CardDefaults.cardColors(containerColor = if (isHeartRate) palette.lemon else Electric, contentColor = chartText)) {
        Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(R.string.recorded_values), fontFamily = YealthRounded, fontWeight = FontWeight.Black, fontSize = 23.sp)
            Text(unit, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 6.dp))
            Row(Modifier.height(132.dp)) {
                Column(Modifier.width(28.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    repeat(4) { n -> Text("%.0f".format(top - (top - bottom) * n / 3), style = MaterialTheme.typography.labelSmall) }
                }
                Canvas(Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = description }) {
                    val inset = 5.dp.toPx()
                    val width = (size.width - inset * 2).coerceAtLeast(1f)
                    val height = size.height - inset * 2
                    val dashed = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
                    repeat(4) { n ->
                        val y = inset + height * n / 3
                        drawLine(grid, Offset(inset, y), Offset(inset + width, y), 1.dp.toPx(), pathEffect = dashed)
                    }
                    repeat(6) { n ->
                        val x = inset + width * n / 5
                        drawLine(grid, Offset(x, inset), Offset(x, inset + height), 1.dp.toPx(), pathEffect = dashed)
                    }
                    drawLine(chartText.copy(alpha=.6f), Offset(inset,inset), Offset(inset,inset+height),1.dp.toPx())
                    drawLine(chartText.copy(alpha=.6f), Offset(inset,inset+height), Offset(inset+width,inset+height),1.dp.toPx())
                    val first = sorted.first().instant.toEpochMilli()
                    val duration = (sorted.last().instant.toEpochMilli() - first).coerceAtLeast(1)
                    var previous: Offset? = null
                    sorted.forEach { point ->
                        val x = if (sorted.size == 1) size.width / 2 else inset + width * ((point.instant.toEpochMilli() - first).toDouble() / duration).toFloat()
                        val y = inset + height * (1 - (point.value - bottom) / (top - bottom)).toFloat()
                        val position = Offset(x, y)
                        previous?.let { drawLine(color, it, position, 1.dp.toPx()) }
                        drawCircle(chartText, 5.dp.toPx(), position)
                        drawCircle(color, 4.dp.toPx(), position)
                        previous = position
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val first = sorted.first().instant.toEpochMilli()
                val duration = sorted.last().instant.toEpochMilli() - first
                repeat(4) { n -> Text(formatter.format(Instant.ofEpochMilli(first + duration * n / 3)), fontSize = 10.sp) }
            }
        }
    }
}

@Composable
fun ChevronBadge() {
    Box(Modifier.size(36.dp).background(LocalContentColor.current.copy(alpha = .08f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(23.dp))
    }
}

@Composable
fun SummaryValue(value: String) {
    val startsNumeric = value.firstOrNull()?.isDigit() == true
    Text(androidx.compose.ui.text.buildAnnotatedString {
        if (startsNumeric) {
            Regex("[0-9][0-9,.]*|[^0-9]+").findAll(value).forEach { match ->
                val number = match.value.first().isDigit()
                pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = if (number) 38.sp else 22.sp,
                    fontWeight = if (number) FontWeight.Bold else FontWeight.Normal))
                append(match.value); pop()
            }
        } else {
            pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold))
            append(value); pop()
        }
    }, style = MaterialTheme.typography.headlineMedium, lineHeight = 39.sp)
}
