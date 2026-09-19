package dev.zdrng.yealth.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zdrng.yealth.ui.feature.RouteAccessViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.zdrng.yealth.YealthApplication
import dev.zdrng.yealth.domain.model.HealthFeature
import dev.zdrng.yealth.domain.model.ProviderStatus
import dev.zdrng.yealth.ui.feature.AccessViewModel
import dev.zdrng.yealth.ui.navigation.YealthApp
import dev.zdrng.yealth.ui.theme.YealthTheme

class MainActivity : ComponentActivity() {
    private lateinit var access: AccessViewModel
    private lateinit var routes: RouteAccessViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as YealthApplication).container
        access = ViewModelProvider(this, viewModelFactory { initializer { AccessViewModel(container.healthBrowser) } })[AccessViewModel::class.java]
        routes = ViewModelProvider(this, viewModelFactory { initializer { RouteAccessViewModel(container.healthBrowser, createSavedStateHandle()) } })[RouteAccessViewModel::class.java]
        val routeLauncher = registerForActivityResult(container.healthActions.routeContract) { routes.result(it) }
        val launcher = registerForActivityResult(container.healthActions.permissionContract) {
            // The returned subset is not the source of truth; also recheck denial/cancellation.
            access.refresh()
        }
        val content = if (intent.getBooleanExtra("preview", false)) container.previewContent else container.browserContent
        setContent { YealthTheme {
            YealthApp(content, access, container.healthBrowser,
                routeState = routes.uiState.collectAsStateWithLifecycle().value,
                requestRoute = { id ->
                    routes.begin(id)
                    try { routeLauncher.launch(id) }
                    catch (_: android.content.ActivityNotFoundException) { routes.failed() }
                    catch (_: SecurityException) { routes.failed() }
                },
                requestPermissions = { permissions ->
                    if (access.uiState.value.environment?.provider == ProviderStatus.AVAILABLE && permissions.isNotEmpty()) {
                        try { launcher.launch(permissions) }
                        catch (_: android.content.ActivityNotFoundException) { access.actionFailed() }
                        catch (_: SecurityException) { access.actionFailed() }
                    }
                },
                requestHistory = {
                    val env = access.uiState.value.environment
                    if (env?.provider == ProviderStatus.AVAILABLE && HealthFeature.HISTORY in env.features && !env.historyGranted) {
                        try { launcher.launch(setOf(container.healthActions.historyPermission)) }
                        catch (_: android.content.ActivityNotFoundException) { access.actionFailed() }
                        catch (_: SecurityException) { access.actionFailed() }
                    }
                },
                openSettings = { if (!container.healthActions.launch(this, container.healthActions.settingsIntent())) access.actionFailed() },
                recover = { if (!container.healthActions.launch(this, container.healthActions.recoveryIntent())) access.actionFailed() })
        } }
    }
    override fun onStart() { super.onStart(); routes.foreground(); access.foreground() }
    override fun onStop() { routes.background(); access.background(); super.onStop() }
}
