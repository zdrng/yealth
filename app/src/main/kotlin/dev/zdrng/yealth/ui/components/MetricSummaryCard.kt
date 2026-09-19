package dev.zdrng.yealth.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Presentation only: labels and values come from the provider summary state. */
@Composable
fun MetricSummaryCard(typeId: String, label: String, value: String?, supporting: String,
    onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false, error: Boolean = false) {
    val palette = dev.zdrng.yealth.ui.theme.LocalMockupColors.current
    val colors = when (typeId) {
        "distance" -> CardDefaults.cardColors(palette.mint, dev.zdrng.yealth.ui.theme.Ink)
        "exercise_session" -> CardDefaults.cardColors(palette.lemon, dev.zdrng.yealth.ui.theme.Ink)
        else -> categoryCardColors(metricCategory(typeId))
    }
    Card(onClick, modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HealthIcon(metricCategory(typeId), prominent = true, typeId = typeId)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.titleMediumEmphasized)
                if (loading) LoadingIndicator(Modifier.size(40.dp))
                else if (!value.isNullOrBlank()) {
                    if (error || value.length > 35) Text(value, style = MaterialTheme.typography.bodyMedium)
                    else SummaryValue(value)
                }
                if (supporting.isNotBlank()) Text(supporting, style = MaterialTheme.typography.labelMedium)
            }
            ChevronBadge()
        }
    }
}
