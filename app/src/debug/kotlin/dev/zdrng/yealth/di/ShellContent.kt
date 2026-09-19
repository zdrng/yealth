package dev.zdrng.yealth.di

import dev.zdrng.yealth.domain.model.RecordType
import dev.zdrng.yealth.ui.feature.BrowserContent
import dev.zdrng.yealth.ui.preview.previewContent

internal fun createBrowserContent(catalog: List<RecordType>): BrowserContent = previewContent(catalog)
