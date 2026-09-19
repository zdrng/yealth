package dev.zdrng.yealth.data.healthconnect

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission

/** Android integration values are wired by the composition root, never imported by UI. */
class HealthConnectActions {
    val routeContract = object : androidx.activity.result.contract.ActivityResultContract<String, dev.zdrng.yealth.domain.model.HealthValue.Items?>() {
        private val delegate = androidx.health.connect.client.contracts.ExerciseRouteRequestContract()
        override fun createIntent(context: Context, input: String): Intent = delegate.createIntent(context, input)
        override fun parseResult(resultCode: Int, intent: Intent?) = delegate.parseResult(resultCode, intent)?.let(::mapRoute)
    }
    val permissionContract = PermissionController.createRequestPermissionResultContract()
    val historyPermission: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
    fun settingsIntent(): Intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
    fun recoveryIntent(): Intent = if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_SETTINGS)
        else Intent(Intent.ACTION_VIEW, "market://details?id=com.google.android.apps.healthdata".toUri())
    fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: android.content.ActivityNotFoundException) { false }
      catch (_: SecurityException) { false }
}
