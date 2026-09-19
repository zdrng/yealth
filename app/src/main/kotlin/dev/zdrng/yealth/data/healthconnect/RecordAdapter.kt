package dev.zdrng.yealth.data.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.zdrng.yealth.domain.model.*
import kotlin.reflect.KClass

/** Compile-time SDK type binding. Planned readers never perform reads or return fake empties. */
class RecordAdapter<T : Record>(
    val recordClass: KClass<T>,
    id: String,
    category: HealthCategory,
    timeShape: TimeShape,
    fields: List<FieldSpec>,
    feature: HealthFeature? = null,
    experimental: Boolean = false,
    private val mapper: ((T) -> HealthRecord)? = null,
) {
    val type = RecordType(
        id, category, timeShape, HealthPermission.getReadPermission(recordClass), fields, feature,
        when {
            experimental -> ReaderStatus.EXPERIMENTAL_REVIEW
            mapper != null -> ReaderStatus.IMPLEMENTED
            else -> ReaderStatus.PLANNED
        },
    )

    suspend fun read(gateway: HealthConnectGateway, request: RecordRequest): PageResult {
        require(request.query.typeId == type.id)
        val mapping = mapper ?: return PageResult.NotImplemented(type.id)
        if (type.readerStatus != ReaderStatus.IMPLEMENTED) return PageResult.NotImplemented(type.id)
        val query = request.query
        val response = gateway.read(
            ReadRecordsRequest(
                recordType = recordClass,
                timeRangeFilter = TimeRangeFilter.between(query.range.start, query.range.end),
                ascendingOrder = query.ascending,
                pageSize = query.pageSize,
                pageToken = request.cursor?.token,
            ),
        )
        return PageResult.Success(
            RecordPage(response.records.map(mapping), response.pageToken?.takeIf { it.isNotEmpty() }?.let { RecordCursor(query, it) }),
        )
    }
}
