
package com.vale.vic.automation.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.vale.vic.automation.VicServiceState

class VicAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        VicServiceState.setConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event handling will be implemented in a later stage.
    }

    override fun onInterrupt() {
        // Called when Android interrupts the service.
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        VicServiceState.setConnected(false)
        return super.onUnbind(intent)
    }
}
