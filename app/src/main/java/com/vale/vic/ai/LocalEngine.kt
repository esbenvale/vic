package com.vale.vic.ai

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.vale.vic.automation.permissions.PermissionRegistry
import com.vale.vic.automation.service.VicServiceState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Offline, deterministic commands. Works with no internet and no API key. */
object LocalEngine {

    fun handle(ctx: Context, input: String): String? {
        val tokens = input.lowercase(Locale.ROOT)
            .split(Regex("[^a-z']+"))
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty() || tokens.size > 6) return null
        val text = tokens.joinToString(" ")

        fun has(vararg words: String): Boolean = words.any { tokens.contains(it) }

        return when {
            text.contains("who are you") || text.contains("your name") ->
                "I am VIC, the VALE Intelligent Companion, Sir."
            has("hello", "hi", "hey") -> "Good day, Sir. How may I assist?"
            has("thanks", "thank") -> "My pleasure, Sir."
            has("help", "commands") || text.contains("what can you do") -> helpText()
            has("time") -> "It is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + ", Sir."
            has("date", "today") ->
                "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + ", Sir."
            has("battery") -> battery(ctx)
            has("accessibility") -> accessibilityStatus(ctx)
            text.contains("last app") -> lastApp()
            has("device") || text.contains("which phone") || text.contains("what phone") -> device()
            else -> null
        }
    }

    fun noAiNotice(): String =
        "I did not recognise that as a built-in command, Sir, and no AI provider is " +
            "configured. Say \"help\" for what I can do offline, or add a provider in " +
            "the AI tab."

    private fun helpText(): String =
        "Offline, I understand: time, date, battery, device, accessibility status, " +
            "\"last app\". With an AI provider configured in the AI tab, I can also " +
            "answer general questions. I cannot yet open apps or operate the screen; " +
            "that is the next milestone."

    private fun battery(ctx: Context): String {
        val intent: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        if (level < 0 || scale <= 0) return "I could not read the battery level, Sir."
        val pct = level * 100 / scale
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return "Battery is at " + pct + "%" + (if (charging) ", and charging." else ".")
    }

    private fun accessibilityStatus(ctx: Context): String {
        val enabled = PermissionRegistry.isAccessibilityEnabled(ctx)
        val connected = VicServiceState.connected
        return when {
            enabled && connected -> "Accessibility is enabled and my service is connected, Sir."
            enabled -> "Accessibility is enabled in Android, but my service is not connected yet."
            else -> "Accessibility is not enabled. You can switch it on from the Setup tab."
        }
    }

    private fun lastApp(): String {
        val pkg = VicServiceState.lastExternalPackage
        return if (pkg == null) {
            "I have not seen another app yet, Sir."
        } else {
            "The last other app I saw was " + pkg + ", Sir."
        }
    }

    private fun device(): String =
        Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE +
            " (API " + Build.VERSION.SDK_INT + ")."
}
