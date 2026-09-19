package dev.zdrng.yealth

import android.app.Application
import dev.zdrng.yealth.di.AppContainer

class YealthApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
