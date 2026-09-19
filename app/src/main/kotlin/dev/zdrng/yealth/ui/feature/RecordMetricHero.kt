package dev.zdrng.yealth.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.zdrng.yealth.ui.theme.Electric
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.zdrng.yealth.R
import dev.zdrng.yealth.domain.model.HealthRecord
import dev.zdrng.yealth.domain.model.HealthValue
import dev.zdrng.yealth.domain.model.RecordField
import dev.zdrng.yealth.domain.model.RecordTime
import dev.zdrng.yealth.ui.components.MetricValue
import dev.zdrng.yealth.ui.components.OrganicShape
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** One original measurement, never a sum, average, score or inferred health interpretation. */
@Composable
fun RecordMetricHero(record: HealthRecord, latest: Boolean = false) {
    val measurement = recordHeroMeasurement(record)
    Card(Modifier.fillMaxWidth().testTag("record-metric-hero"), shape = OrganicShape,
        colors = CardDefaults.cardColors(containerColor = Electric, contentColor = Color.White)) {
        Column(Modifier.padding(horizontal = 28.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(if (latest) R.string.latest_record else R.string.record_details),
                style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .85f))
            if (measurement != null) {
                measurement.qualifier?.let { Text(stringResource(it), style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = .85f)) }
                MetricValue(measurement.value, Modifier.testTag("record-metric-value"), unit = measurement.unit)
                measurement.title?.let { Text(it, style = MaterialTheme.typography.titleMediumEmphasized) }
            } else {
                // Categorical codes, text and missing measurements are not presented as quantities.
                MetricValue(recordValue(record), modifier = Modifier.testTag("record-metric-value"), numeric = false)
            }
            if (latest) Text(heroTime(record, measurement?.sampleTime), style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = .85f), modifier = Modifier.testTag("record-metric-time"))
        }
    }
}

private data class HeroMeasurement(
    val value: String,
    val unit: String,
    val qualifier: Int? = null,
    val title: String? = null,
    val sampleTime: Instant? = null,
)

/** Field selection operates on typed source values; rendered strings are never parsed. */
private fun recordHeroMeasurement(record: HealthRecord): HeroMeasurement? {
    fun field(key: String) = record.fields.firstOrNull { it.key == key }
    fun numeric(key: String) = field(key)?.value?.let(::measurementNumber)
    fun text(key: String) = (field(key)?.value as? HealthValue.Text)?.value?.takeIf(String::isNotBlank)
    if (record.typeId.endsWith("_session")) {
        val interval = record.time as? RecordTime.Interval ?: return null
        val duration = Duration.between(interval.start, interval.end)
        if (duration.isNegative) return null
        val wholeMinutes = duration.nano == 0 && duration.seconds % 60L == 0L
        val number = if (wholeMinutes) formatNumber(duration.seconds / 60)
            else formatNumber(BigDecimal.valueOf(duration.seconds).add(BigDecimal.valueOf(duration.nano.toLong(), 9)))
        return HeroMeasurement(number, if (wholeMinutes) "min" else "s", R.string.metric_session_duration, text("title"))
    }
    if (record.typeId == "blood_pressure") {
        val systolic = numeric("systolic")
        val diastolic = numeric("diastolic")
        if (systolic != null && diastolic != null && systolic.unit == diastolic.unit) {
            return HeroMeasurement(systolic.value + "/" + diastolic.value, systolic.unit, R.string.metric_blood_pressure_pair)
        }
    }
    // Skin temperature baseline and deltas are distinct measures. Do not turn a delta into
    // an absolute temperature or combine the baseline and sample into a derived value.
    if (record.typeId == "skin_temperature") {
        numeric("baseline")?.let { return it.copy(qualifier = R.string.metric_baseline_temperature) }
    }
    val sample = record.samples.maxByOrNull { it.time }
    if (sample != null) {
        primaryNumeric(sample.fields, record.typeId)?.let { value ->
            return value.copy(sampleTime = sample.time, qualifier = if (record.typeId == "skin_temperature")
                R.string.metric_latest_temperature_delta else R.string.metric_latest_sample)
        }
    }
    val selected = primaryNumeric(record.fields, record.typeId) ?: return null
    return if (record.typeId == "nutrition") selected.copy(
        title = listOfNotNull(selected.title, text("name")).joinToString(" · ").takeIf(String::isNotBlank),
    ) else selected
}

private fun primaryNumeric(fields: List<RecordField>, typeId: String): HeroMeasurement? {
    val keys = when (typeId) {
        "nutrition" -> listOf("energy", "protein", "totalCarbohydrate", "totalFat")
        "skin_temperature" -> listOf("delta", "baseline")
        else -> listOf("count", "beatsPerMinute", "vo2MillilitersPerMinuteKilogram", "distance", "energy", "mass", "weight",
            "height", "percentage", "temperature", "level", "volume", "rate", "power", "speed", "floors", "elevation",
            "basalMetabolicRate", "heartRateVariabilityMillis", "revolutionsPerMinute")
    }
    val field = keys.firstNotNullOfOrNull { key -> fields.firstOrNull { it.key == key && measurementNumber(it.value) != null } }
        ?: fields.firstOrNull { measurementNumber(it.value) != null } ?: return null
    val value = measurementNumber(field.value) ?: return null
    val qualifier = if (typeId == "nutrition") when (field.key) {
        "energy" -> R.string.metric_energy
        "protein" -> R.string.metric_protein
        "totalCarbohydrate" -> R.string.metric_carbohydrate
        "totalFat" -> R.string.metric_fat
        else -> null
    } else null
    // A nutrition micronutrient must retain its identity when none of the preferred fields exists.
    return value.copy(qualifier = qualifier,
        title = if (typeId == "nutrition" && qualifier == null) field.key.replace(Regex("([a-z])([A-Z])"), "$1 $2") else null)
}

private fun measurementNumber(value: HealthValue): HeroMeasurement? = when (value) {
    is HealthValue.Integer -> HeroMeasurement(formatNumber(value.value), value.unit.orEmpty())
    is HealthValue.Decimal -> if (value.value.isFinite()) HeroMeasurement(formatNumber(BigDecimal.valueOf(value.value)), value.unit) else null
    else -> null
}

private fun formatNumber(number: Number): String = NumberFormat.getNumberInstance().apply {
    // The display preserves the supplied numeric precision rather than imposing a rounded summary.
    maximumFractionDigits = 340
}.format(number)

@Composable
private fun heroTime(record: HealthRecord, sampleTime: Instant?): String {
    val format = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss XXX")
    fun formatted(time: Instant, offset: ZoneOffset?) = format.withZone(offset ?: ZoneOffset.UTC).format(time)
    if (sampleTime != null) return stringResource(R.string.metric_sample_time, formatted(sampleTime, ZoneOffset.UTC))
    return when (val time = record.time) {
        is RecordTime.Point -> formatted(time.time, time.offset)
        is RecordTime.Interval -> stringResource(R.string.metric_record_interval,
            formatted(time.start, time.startOffset), formatted(time.end, time.endOffset))
    }
}
