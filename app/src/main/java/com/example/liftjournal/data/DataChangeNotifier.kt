package com.example.liftjournal.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Signals that user data was committed (entry saved or deleted, goal or setting changed).
 * Real-time backup listens to it. Several quick changes may collapse into one signal.
 */
class DataChangeNotifier {
    private val signals =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val changes: SharedFlow<Unit> = signals.asSharedFlow()

    fun notifyChanged() {
        signals.tryEmit(Unit)
    }
}
