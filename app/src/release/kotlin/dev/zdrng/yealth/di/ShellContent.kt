package dev.zdrng.yealth.di

import dev.zdrng.yealth.domain.model.RecordType
import dev.zdrng.yealth.ui.feature.BrowserContent

internal fun createBrowserContent(catalog: List<RecordType>): BrowserContent = BrowserContent(catalog)
