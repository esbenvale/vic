package com.vale.vic.automation.observer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Holds the latest reading in memory only. Nothing is saved or sent anywhere. */
object ObserverState {
    var latest by mutableStateOf<ScreenObservation?>(null)
    var waiting by mutableStateOf(false)

    private val handler = Handler(Looper.getMainLooper())

    /** Call from the main thread. */
    fun captureNow() {
        latest = ScreenObserver.observe()
    }

    /** Waits, then reads whichever app is in front. Call from the main thread. */
    fun captureDelayed(context: Context, delayMs: Long) {
        if (waiting) return
        waiting = true
        val app = context.applicationContext
        handler.postDelayed({
            val obs = ScreenObserver.observe()
            latest = obs
            waiting = false
            val msg = if (obs.ok) {
                "VIC read " + obs.elements.size + " elements in " + obs.packageName
            } else {
                "VIC could not read the screen: " + obs.message
            }
            Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
        }, delayMs)
    }
}
