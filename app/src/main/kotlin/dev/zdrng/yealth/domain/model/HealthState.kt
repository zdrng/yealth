package dev.zdrng.yealth.domain.model

enum class ProviderStatus { AVAILABLE, UNAVAILABLE, UPDATE_REQUIRED }
enum class ReadFailure { ACCESS_CHANGED, PROVIDER_ERROR, INVALID_REQUEST }
enum class AccessLevel { NOT_REQUESTABLE, NONE, PARTIAL, GRANTED }

data class PermissionState(val required: Set<String>, val granted: Set<String>) {
    val missing: Set<String> get() = required - granted
    val level: AccessLevel get() = when {
        required.isEmpty() -> AccessLevel.NOT_REQUESTABLE
        missing.isEmpty() -> AccessLevel.GRANTED
        (required intersect granted).isEmpty() -> AccessLevel.NONE
        else -> AccessLevel.PARTIAL
    }
}

data class HealthEnvironment(
    val provider: ProviderStatus,
    val features: Set<HealthFeature> = emptySet(),
    val grantedPermissions: Set<String> = emptySet(),
    val historyGranted: Boolean = false,
)

sealed interface EnvironmentResult {
    data class Ready(val environment: HealthEnvironment) : EnvironmentResult
    data class Failed(val reason: ReadFailure) : EnvironmentResult
}

sealed interface PageResult {
    data class Success(val page: RecordPage) : PageResult
    data class Failed(val reason: ReadFailure) : PageResult
    data class NotImplemented(val typeId: String) : PageResult
}

/** Framework-free screen state; ViewModels own loading and stale-request cancellation. */
sealed interface RecordsState {
    data object Loading : RecordsState
    data class Content(val page: RecordPage, val historyLimited: Boolean) : RecordsState
    data class Empty(val historyLimited: Boolean) : RecordsState
    data class AccessRequired(val permissions: PermissionState) : RecordsState
    data class ProviderUnavailable(val status: ProviderStatus) : RecordsState
    data class UnsupportedFeature(val feature: HealthFeature) : RecordsState
    data object HistoryAccessRequired : RecordsState
    data class NotImplemented(val typeId: String) : RecordsState
    data class Failed(val reason: ReadFailure) : RecordsState
}
