package dev.zdrng.yealth.data.healthconnect

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.metadata.Metadata
import dev.zdrng.yealth.domain.model.*

internal fun Metadata.toDomain() = RecordMetadata(
    id = id,
    originPackage = dataOrigin.packageName,
    lastModified = lastModifiedTime,
    clientRecordId = clientRecordId,
    clientRecordVersion = clientRecordVersion,
    recordingMethod = recordingMethod,
    device = device?.let { HealthDevice(it.type, it.manufacturer, it.model) },
)

internal fun mapSteps(record: StepsRecord) = HealthRecord(
    typeId = "steps",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(RecordField("count", HealthValue.Integer(record.count, "steps"))),
)

internal fun mapVo2Max(record: Vo2MaxRecord) = HealthRecord(
    typeId = "vo2_max",
    time = RecordTime.Point(record.time, record.zoneOffset),
    metadata = record.metadata.toDomain(),
    fields = listOf(
        RecordField("vo2MillilitersPerMinuteKilogram", HealthValue.Decimal(record.vo2MillilitersPerMinuteKilogram, "mL/(min·kg)")),
        RecordField("measurementMethod", HealthValue.Code(record.measurementMethod)),
    ),
)

internal fun mapHeartRate(record: HeartRateRecord) = HealthRecord(
    typeId = "heart_rate",
    time = RecordTime.Interval(record.startTime, record.endTime, record.startZoneOffset, record.endZoneOffset),
    metadata = record.metadata.toDomain(),
    fields = emptyList(),
    samples = record.samples.map {
        RecordSample(it.time, listOf(RecordField("beatsPerMinute", HealthValue.Integer(it.beatsPerMinute, "bpm"))))
    },
)
