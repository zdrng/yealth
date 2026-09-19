package dev.zdrng.yealth.domain.model

import java.time.Instant
import java.time.ZoneOffset

enum class HealthCategory {
    ACTIVITY, BODY_MEASUREMENTS, CYCLE_TRACKING, NUTRITION, SLEEP, VITALS, WELLNESS,
}

enum class TimeShape { INSTANT, INTERVAL, SERIES }
enum class HealthFeature { HISTORY, SKIN_TEMPERATURE, PLANNED_EXERCISE, MINDFULNESS }
enum class ReaderStatus { IMPLEMENTED, PLANNED, EXPERIMENTAL_REVIEW }

/** Schema identifiers are stable keys, not localized UI labels. */
data class FieldSpec(val key: String, val sdkValueType: String)

data class RecordType(
    val id: String,
    val category: HealthCategory,
    val timeShape: TimeShape,
    val readPermission: String,
    val fields: List<FieldSpec>,
    val feature: HealthFeature? = null,
    val readerStatus: ReaderStatus = ReaderStatus.PLANNED,
)

/** Keep integral counts integral; do not round measurements at the integration boundary. */
sealed interface HealthValue {
    data class Integer(val value: Long, val unit: String? = null) : HealthValue
    data class Decimal(val value: Double, val unit: String) : HealthValue
    data class Text(val value: String) : HealthValue
    data class Code(val value: Int) : HealthValue
    data class Flag(val value: Boolean) : HealthValue
    data class Timestamp(val value: Instant) : HealthValue
    data class Offset(val value: ZoneOffset) : HealthValue
    data class Fields(val fields: List<RecordField>) : HealthValue
    data class Items(val values: List<HealthValue>) : HealthValue
    data object Missing : HealthValue
}

data class RecordField(val key: String, val value: HealthValue)

sealed interface RecordTime {
    data class Point(val time: Instant, val offset: ZoneOffset?) : RecordTime
    data class Interval(
        val start: Instant,
        val end: Instant,
        val startOffset: ZoneOffset?,
        val endOffset: ZoneOffset?,
    ) : RecordTime
}

data class HealthDevice(val type: Int, val manufacturer: String?, val model: String?)

data class RecordMetadata(
    val id: String,
    val originPackage: String,
    val lastModified: Instant,
    val clientRecordId: String?,
    val clientRecordVersion: Long,
    val recordingMethod: Int,
    val device: HealthDevice?,
)

data class RecordSample(val time: Instant, val fields: List<RecordField>)

data class HealthRecord(
    val typeId: String,
    val time: RecordTime,
    val metadata: RecordMetadata,
    val fields: List<RecordField>,
    val samples: List<RecordSample> = emptyList(),
)

/** Half-open range [start, end), resolved by the UI's selected local date and zone. */
data class HealthTimeRange(val start: Instant, val end: Instant) {
    init { require(start < end) { "Range start must precede its end" } }
}

data class RecordQuery(
    val typeId: String,
    val range: HealthTimeRange,
    val pageSize: Int = 100,
    val ascending: Boolean = false,
    // Explicit full-history intent, not an inaccurate 'now minus 30 days' calculation.
    val requireFullHistory: Boolean = false,
) {
    init { require(pageSize in 1..1000) { "Page size must be between 1 and 1000" } }
}

/** A token is valid only for the exact query that produced it. Never persist it. */
data class RecordCursor(val query: RecordQuery, val token: String)
data class RecordRequest(val query: RecordQuery, val cursor: RecordCursor? = null) {
    init { require(cursor == null || cursor.query == query) { "Cursor belongs to another query" } }
}

data class RecordPage(val records: List<HealthRecord>, val next: RecordCursor?)
