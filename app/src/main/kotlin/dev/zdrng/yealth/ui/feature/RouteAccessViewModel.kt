package dev.zdrng.yealth.ui.feature

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.service.HealthBrowserService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RouteUiState(val recordId: String? = null, val locations: HealthValue.Items? = null, val cancelled: Boolean = false, val failed: Boolean = false)

/** Only the pending record identifier survives recreation; never persist route coordinates. */
class RouteAccessViewModel(private val service: HealthBrowserService, private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(RouteUiState())
    val uiState = mutableState.asStateFlow()
    private var check: Job? = null
    private var foreground = false
    fun foreground() { foreground = true }
    fun background() { foreground = false; check?.cancel(); mutableState.value = RouteUiState() }
    fun begin(recordId: String) { saved["routeRecord"] = recordId; mutableState.value = RouteUiState(recordId) }
    fun result(locations: HealthValue.Items?) {
        val id = saved.remove<String>("routeRecord") ?: return
        check?.cancel()
        check = viewModelScope.launch {
            val env = (service.environment() as? EnvironmentResult.Ready)?.environment
            ensureActive()
            val permission = service.recordTypes.single { it.id == "exercise_session" }.readPermission
            if (!foreground || env?.provider != ProviderStatus.AVAILABLE || permission !in env.grantedPermissions) return@launch
            mutableState.value = RouteUiState(id, locations, cancelled = locations == null)
        }
    }
    fun failed() { val id = saved.remove<String>("routeRecord"); mutableState.value = RouteUiState(id, failed = true) }
}
