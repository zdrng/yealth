package dev.zdrng.yealth.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.service.HealthBrowserService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AccessUiState(
    val foreground: Boolean = false,
    val result: EnvironmentResult? = null,
    val revision: Long = 0,
    val actionFailed: Boolean = false,
) {
    val environment: HealthEnvironment? get() = (result as? EnvironmentResult.Ready)?.environment
}

class AccessViewModel(private val service: HealthBrowserService, scope: CoroutineScope? = null) : ViewModel() {
    private val workScope = scope ?: viewModelScope
    private var check: Job? = null
    private val mutableState = MutableStateFlow(AccessUiState())
    val uiState = mutableState.asStateFlow()

    fun foreground() {
        mutableState.value = uiState.value.copy(foreground = true)
        refresh()
    }
    fun background() {
        check?.cancel()
        mutableState.value = AccessUiState(revision = uiState.value.revision + 1)
    }
    fun refresh() {
        if (!uiState.value.foreground) return
        check?.cancel()
        mutableState.value = uiState.value.copy(result = null, revision = uiState.value.revision + 1)
        check = workScope.launch {
            val result = service.environment()
            ensureActive()
            mutableState.value = uiState.value.copy(result = result)
        }
    }
    fun actionFailed() { mutableState.value = uiState.value.copy(actionFailed = true) }
    fun clearActionFailure() { mutableState.value = uiState.value.copy(actionFailed = false) }
    fun permissions(category: HealthCategory): PermissionState = uiState.value.environment?.let {
        if (it.provider == ProviderStatus.AVAILABLE) service.permissionsFor(category, it) else null
    } ?: PermissionState(emptySet(), emptySet())
}

/** Shared permission strings do not imply that every associated reader is implemented. */
fun accessTypes(catalog: List<RecordType>, category: HealthCategory, permissions: PermissionState): List<RecordType> =
    catalog.filter { it.category == category && it.readerStatus == ReaderStatus.IMPLEMENTED && it.readPermission in permissions.required }
