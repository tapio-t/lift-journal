package com.example.liftjournal

import android.app.Application
import android.content.Context

/** Application class; owns the [AppContainer]. */
class LiftJournalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** The app container from any context. */
val Context.appContainer: AppContainer get() = (applicationContext as LiftJournalApp).container
