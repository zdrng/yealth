package dev.zdrng.yealth.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.aggregate.AggregationResult

/** Small SDK seam allows ordinary JVM tests without a Binder service or a mocking library. */
interface HealthConnectGateway {
    fun sdkStatus(): Int
    fun featureStatus(feature: Int): Int
    suspend fun grantedPermissions(): Set<String>
    suspend fun <T : Record> read(request: ReadRecordsRequest<T>): ReadRecordsResponse<T>
    suspend fun aggregate(request: AggregateRequest): AggregationResult
}

class AndroidHealthConnectGateway(context: Context) : HealthConnectGateway {
    private val applicationContext = context.applicationContext

    // Never cache SDK availability; installing/updating the provider must recover on refresh.
    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(applicationContext)

    override fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(applicationContext)
    override fun featureStatus(feature: Int): Int = client().features.getFeatureStatus(feature)
    override suspend fun grantedPermissions(): Set<String> = client().permissionController.getGrantedPermissions()
    override suspend fun <T : Record> read(request: ReadRecordsRequest<T>): ReadRecordsResponse<T> =
        client().readRecords(request)
    override suspend fun aggregate(request: AggregateRequest): AggregationResult = client().aggregate(request)
}
