package com.vale.vic.automation.permissions

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.vale.vic.automation.service.VicAccessibilityService

data class PermissionEntry(
    val id: String,
    val title: String,
    val why: String,
    val enabledInAndroid: Boolean
)

object PermissionRegistry {

    const val ACCESSIBILITY = "accessibility"

    fun isAccessibilityEnabled(context: Context): Boolean {
        val self = ComponentName(context, VicAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { entry ->
            ComponentName.unflattenFromString(entry) == self
        }
    }

    fun accessibility(context: Context) = PermissionEntry(
        id = ACCESSIBILITY,
        title = "Accessibility automation",
        why = "Lets VIC see the real screen and, while you run a task, " +
            "tap, scroll and type for you so you can watch it work. " +
            "VIC cannot turn this on by itself, and you can revoke it " +
            "at any time in Android settings.",
        enabledInAndroid = isAccessibilityEnabled(context)
    )
}
