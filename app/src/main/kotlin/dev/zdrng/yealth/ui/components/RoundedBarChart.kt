package dev.zdrng.yealth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.zdrng.yealth.R
import java.text.NumberFormat

data class RecordedBar(val label: String, val value: Double?, val accessibilityLabel: String = label)

/** A true zero baseline; absent observations remain gaps. Colors carry no goal or health meaning. */
@Composable
fun RoundedBarChart(bars: List<RecordedBar>, unit: String, modifier: Modifier = Modifier) {
    if (bars.isEmpty()) return
    val values = bars.mapNotNull { it.value?.takeIf { number -> number.isFinite() && number >= 0 } }
    val maximum = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    val largeText = LocalDensity.current.fontScale > 1.3f
    val primary = MaterialTheme.colorScheme.primary
    val baseline = MaterialTheme.colorScheme.outlineVariant
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(unit, style = MaterialTheme.typography.labelMedium)
            Text(format.format(maximum), style = MaterialTheme.typography.labelMedium)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bars.forEach { bar ->
                val valid = bar.value?.takeIf { it.isFinite() && it >= 0 }
                val missing = stringResource(R.string.not_provided)
                val description = if (bar.accessibilityLabel != bar.label) bar.accessibilityLabel
                    else bar.label + ": " + (valid?.let { format.format(it) + " " + unit } ?: missing)
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.fillMaxWidth().height(208.dp)) {
                        drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                        if (valid != null && valid > 0) {
                            val height = (size.height * valid / maximum).toFloat()
                            drawRoundRect(primary,
                                topLeft = Offset(0f, size.height - height), size = Size(size.width, height),
                                cornerRadius = CornerRadius(size.width / 2, size.width / 2))
                        }
                    }
                    Text(bar.label, style = MaterialTheme.typography.labelMedium)
                    if (!largeText) Text(valid?.let(format::format) ?: "—", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text("0", style = MaterialTheme.typography.labelSmall)
        if (largeText) bars.forEachIndexed { index, bar ->
            // Full-width rows keep digits together when seven large value labels cannot fit.
            val value = bar.value?.let { format.format(it) + " " + unit } ?: stringResource(R.string.not_provided)
            Text(if (bar.accessibilityLabel != bar.label) bar.accessibilityLabel else "${bar.label}: $value",
                modifier = Modifier.testTag("chart-value-$index"), style = MaterialTheme.typography.bodySmall)
        }
    }
}
