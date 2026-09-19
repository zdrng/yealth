package dev.zdrng.yealth.di

import android.content.Context
import dev.zdrng.yealth.data.healthconnect.AndroidHealthConnectGateway
import dev.zdrng.yealth.data.healthconnect.HealthConnectRepository
import dev.zdrng.yealth.domain.repository.HealthRepository
import dev.zdrng.yealth.domain.service.HealthBrowserService

/** Constructing the graph never requests permissions or reads health data. */
class AppContainer(context: Context) {
    val healthRepository: HealthRepository by lazy {
        HealthConnectRepository(AndroidHealthConnectGateway(context.applicationContext))
    }
    val healthBrowser: HealthBrowserService by lazy { HealthBrowserService(healthRepository) }
    val browserContent by lazy { dev.zdrng.yealth.ui.feature.BrowserContent(healthBrowser.recordTypes) }
    val previewContent by lazy { createBrowserContent(healthBrowser.recordTypes) }
    val healthActions by lazy { dev.zdrng.yealth.data.healthconnect.HealthConnectActions() }
}
