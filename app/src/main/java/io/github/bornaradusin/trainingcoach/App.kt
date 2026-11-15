package io.github.bornaradusin.trainingcoach

import android.app.Application
import android.content.Context
import io.github.bornaradusin.trainingcoach.data.Catalog
import io.github.bornaradusin.trainingcoach.data.Store
import io.github.bornaradusin.trainingcoach.data.Updates

class App : Application() {
    val catalog by lazy { Catalog(this) }
    val store by lazy { Store(this) }
    val updates by lazy { Updates(this) }

    override fun onCreate() {
        super.onCreate()
        Updates.cleanLeftovers(this)
    }
}

val Context.app: App get() = applicationContext as App
