package dev.zdrng.yealth.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/** One accessible value, with the measurement visually leading its unit. Never ellipsized. */
@Composable
fun MetricValue(value: String, modifier: Modifier = Modifier, unit: String = "", numeric: Boolean = true) {
    val numberStyle = if (numeric) MaterialTheme.typography.displayLarge
        else MaterialTheme.typography.headlineLargeEmphasized
    Text(buildAnnotatedString {
        withStyle(numberStyle.copy(fontWeight = FontWeight.Bold).toSpanStyle()) { append(value) }
        if (unit.isNotBlank()) {
            append(" ")
            withStyle(MaterialTheme.typography.headlineSmall.toSpanStyle()) { append(unit) }
        }
    }, modifier.fillMaxWidth(), style = numberStyle)
}
