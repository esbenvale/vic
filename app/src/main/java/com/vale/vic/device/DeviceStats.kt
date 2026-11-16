package com.vale.vic.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import com.vale.vic.automation.actions.AppFinder
import java.text.SimpleDateFormat
import java.util.Locale

data class StatSection(val title: String, val lines: List<String>)

/** Read-only device information plus plain advice. Changes nothing. */
object DeviceStats {

    private class Battery(
        val level: Int,
        val charging: Boolean,
        val statusText: String,
        val plugText: String,
        val healthText: String,
        val healthOk: Boolean,
        val tempC: Double?,
        val voltageMv: Int?,
        val tech: String,
        val cycles: Int?
    )

    private fun readBattery(ctx: Context): Battery? {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val health = i.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val volt = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val tech = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "unknown"
        val cycles = if (Build.VERSION.SDK_INT >= 34) {
            i.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
        } else {
            -1
        }

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }
        val plugText = when {
            (plugged and BatteryManager.BATTERY_PLUGGED_AC) != 0 -> "AC charger"
            (plugged and BatteryManager.BATTERY_PLUGGED_USB) != 0 -> "USB"
            (plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0 -> "Wireless"
            else -> "Not plugged in"
        }
        val healthText = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Too cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure reported"
            else -> "Unknown"
        }
        return Battery(
            level = pct,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            statusText = statusText,
            plugText = plugText,
            healthText = healthText,
            healthOk = health == BatteryManager.BATTERY_HEALTH_GOOD || health == 0,
            tempC = if (temp == Int.MIN_VALUE) null else temp / 10.0,
            voltageMv = if (volt > 0) volt else null,
            tech = tech,
            cycles = if (cycles >= 0) cycles else null
        )
    }

    private fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)

    private fun gb(bytes: Long): String = String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)

    private fun thermalText(ctx: Context): String? {
        if (Build.VERSION.SDK_INT < 29) return null
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return when (pm.cu
