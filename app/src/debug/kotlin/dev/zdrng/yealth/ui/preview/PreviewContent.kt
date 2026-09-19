package dev.zdrng.yealth.ui.preview

import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.ui.feature.BrowserContent
import java.time.LocalDate
import java.time.ZoneOffset

/** Entirely synthetic and deliberately named. This file is excluded from release. */
fun previewContent(catalog: List<RecordType>): BrowserContent {
    val date = LocalDate.of(2026, 9, 18)
    val offset = ZoneOffset.ofHours(2)
    fun record(type: RecordType, day: LocalDate, index: Int = 0): HealthRecord {
        val start = day.atTime(10, 24).toInstant(offset)
        val fields = when (type.id) {
            "steps" -> listOf(RecordField("count", HealthValue.Integer(listOf(6842L, 10320L, 5240L, 12860L, 3920L, 8910L, 7120L)[index], "steps")))
            "distance" -> listOf(RecordField("distance", HealthValue.Decimal(4800.0, "m")))
            "vo2_max" -> listOf(RecordField("vo2MillilitersPerMinuteKilogram", HealthValue.Decimal(listOf(43.2, 42.9, 43.4, 42.8, 43.1, 42.2)[index], "mL/(min·kg)")), RecordField("measurementMethod", HealthValue.Code(0)))
            "resting_heart_rate" -> listOf(RecordField("beatsPerMinute", HealthValue.Integer(listOf(59L, 57L, 60L, 59L, 61L, 58L)[index], "bpm")))
            "exercise_session" -> listOf(RecordField("title", HealthValue.Text("Sample walk")), RecordField("notes", HealthValue.Missing))
            "weight" -> listOf(RecordField("weight", HealthValue.Decimal(72.4, "kg")))
            "hydration" -> listOf(RecordField("volume", HealthValue.Decimal(250.0, "mL")))
            "nutrition" -> listOf(RecordField("name", HealthValue.Text("Sample meal")), RecordField("energy", HealthValue.Decimal(420.0, "kcal")), RecordField("protein", HealthValue.Missing))
            else -> emptyList()
        }
        val time = if (type.timeShape == TimeShape.INSTANT) RecordTime.Point(start, offset)
            else RecordTime.Interval(start.minusSeconds(if (type.id == "sleep_session") 26640 else 2520), start, offset, offset)
        val samples = if (type.timeShape == TimeShape.SERIES) listOf(0L, 30L, 60L).mapIndexed { n, seconds ->
            RecordSample(start.minusSeconds(120 - seconds), listOf(RecordField(if (type.id == "heart_rate") "beatsPerMinute" else "sample", HealthValue.Integer(68L + n, if (type.id == "heart_rate") "bpm" else null))))
        } else emptyList()
        return HealthRecord(type.id, time,
            RecordMetadata("sample-${type.id}-$day", "dev.yealth.sample", start.plusSeconds(60), null, 0, 0,
                HealthDevice(0, "Sample", "Demo device")), fields, samples)
    }
    val records = catalog.flatMap { type ->
        when (type.id) {
            "steps" -> (0L..6L).mapIndexed { i, days -> record(type, date.minusDays(days), i) }
            "vo2_max", "resting_heart_rate" -> listOf(0L, 9L, 16L, 23L, 30L, 60L).mapIndexed { i, days -> record(type, date.minusDays(days), i) }
            "nutrition", "hydration" -> listOf(record(type, date.minusDays(1)))
            else -> listOf(record(type, date))
        }
    }
    return BrowserContent(catalog, records, isPreview = true, initialDate = date)
}
