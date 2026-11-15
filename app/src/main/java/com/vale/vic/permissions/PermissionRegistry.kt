
package com.vale.vic.permissions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.vale.vic.automation.service.VicAccessibilityService

object PermissionRegistry {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expectedComponent =
            "${context.packageName}/${VicAccessibilityService::class.java.name}"

        return enabledServices.split(':').any {
            it.equals(expectedComponent, ignoreCase = true)
        }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
