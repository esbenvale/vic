package com.vale.vic.automation.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.vale.vic.automation.actions.ActionExecutor

class VicAccessibilityService : AccessibilityService() {

    companion object {
        /** The connected service, or null when Android has not started it. */
        @Volatile
        var instance: VicAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        VicServiceState.connected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        ActionExecutor.onForegroundChanged(pkg, packageName)
        if (pkg != packageName) {
            VicServiceState.lastExternalPackage = pkg
        }
    }

    override fun onInterrupt() {
        ActionExecutor.stop("Android interrupted the service, so I stopped.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        VicServiceState.connected = false
        ActionExecutor.stop("The accessibility service was switched off, so I stopped.")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        VicServiceState.connected = false
        super.onDestroy()
    }
}
