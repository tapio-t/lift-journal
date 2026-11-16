package com.example.liftjournal

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Application class; owns the [AppContainer] and starts real-time backup. */
class LiftJournalApp : Application() {
    lateinit var container: AppContainer
        private set

    /** Lives as long as the process; used for app-wide background work such as watching for data changes. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.backupManager.start(appScope)
        ProcessLifecycleOwner.get().lifecycle.addObserver(BackupOnStop(container))
    }
}

/** Writes a pending backup as soon as the whole app goes to the background (PLAN phase 6, ON_STOP flush). */
private class BackupOnStop(private val container: AppContainer) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) = container.backupManager.flushPendingChange()
}

/** The app container from any context. */
val Context.appContainer: AppContainer get() = (applicationContext as LiftJournalApp).container
