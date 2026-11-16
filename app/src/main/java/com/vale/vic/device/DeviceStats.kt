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
        return when (pm.currentThermalStatus) {
            0 -> "Normal"
            1 -> "Light throttling"
            2 -> "Moderate throttling"
            3 -> "Severe throttling"
            4 -> "Critical"
            5 -> "Emergency"
            6 -> "Shutdown imminent"
            else -> "Unknown"
        }
    }

    private fun storageBytes(): Pair<Long, Long>? {
        return try {
            val s = StatFs(Environment.getDataDirectory().path)
            Pair(s.totalBytes, s.availableBytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun memoryInfo(ctx: Context): ActivityManager.MemoryInfo {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi
    }

    private fun uptimeText(): String {
        val ms = SystemClock.elapsedRealtime()
        val d = ms / 86_400_000L
        val h = (ms / 3_600_000L) % 24
        val m = (ms / 60_000L) % 60
        return d.toString() + "d " + h + "h " + m + "m"
    }

    private fun uptimeDays(): Long = SystemClock.elapsedRealtime() / 86_400_000L

    private fun brightness(ctx: Context): Pair<Int, Boolean>? {
        return try {
            val b = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            val mode = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
            Pair(b * 100 / 255, mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
        } catch (e: Exception) {
            null
        }
    }

    private fun patchAgeDays(): Long? {
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(Build.VERSION.SECURITY_PATCH)
            if (d == null) null else (System.currentTimeMillis() - d.time) / 86_400_000L
        } catch (e: Exception) {
            null
        }
    }

    // ---------- sections ----------

    fun batterySection(ctx: Context): StatSection {
        val b = readBattery(ctx)
            ?: return StatSection("Battery", listOf("Battery information is not available."))
        val lines = ArrayList<String>()
        lines.add("Level: " + (if (b.level >= 0) b.level.toString() + "%" else "unknown"))
        lines.add("Status: " + b.statusText)
        lines.add("Power source: " + b.plugText)
        lines.add("Health (Android's rating): " + b.healthText)
        val t = b.tempC
        if (t != null) lines.add("Temperature: " + fmt1(t) + " °C")
        val v = b.voltageMv
        if (v != null) lines.add("Voltage: " + v + " mV")
        lines.add("Technology: " + b.tech)
        val c = b.cycles
        if (c != null) lines.add("Charge cycles: " + c)
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        lines.add("Battery Saver: " + (if (pm.isPowerSaveMode) "On" else "Off"))
        val thermal = thermalText(ctx)
        if (thermal != null) lines.add("Thermal state: " + thermal)
        return StatSection("Battery", lines)
    }

    fun storageSection(): StatSection {
        val s = storageBytes()
            ?: return StatSection("Storage", listOf("Storage information is not available."))
        val total = s.first
        val free = s.second
        val usedPct = if (total > 0) ((total - free) * 100 / total).toInt() else 0
        return StatSection(
            "Storage",
            listOf(
                "Total: " + gb(total),
                "Free: " + gb(free),
                "Used: " + usedPct + "%"
            )
        )
    }

    fun memorySection(ctx: Context): StatSection {
        val mi = memoryInfo(ctx)
        return StatSection(
            "Memory (RAM)",
            listOf(
                "Total: " + gb(mi.totalMem),
                "Available now: " + gb(mi.availMem),
                "Low memory: " + (if (mi.lowMemory) "Yes" else "No")
            )
        )
    }

    fun systemSection(ctx: Context): StatSection {
        val lines = ArrayList<String>()
        lines.add("Device: " + Build.MANUFACTURER + " " + Build.MODEL)
        lines.add("Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
        lines.add("Security patch: " + Build.VERSION.SECURITY_PATCH)
        lines.add("Uptime: " + uptimeText())
        lines.add("CPU cores: " + Runtime.getRuntime().availableProcessors())
        val abi = if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0] else "unknown"
        lines.add("CPU architecture: " + abi)
        val dm = ctx.resources.displayMetrics
        lines.add("Display: " + dm.widthPixels + " x " + dm.heightPixels + " px, " + dm.densityDpi + " dpi")
        val br = brightness(ctx)
        if (br != null) {
            lines.add("Brightness: about " + br.first + "% (" + (if (br.second) "adaptive" else "manual") + ")")
        }
        lines.add("Launchable apps: " + AppFinder.launchableApps(ctx).size)
        return StatSection("System", lines)
    }

    fun sections(ctx: Context): List<StatSection> {
        val out = ArrayList<StatSection>()
        out.add(batterySection(ctx))
        out.add(storageSection())
        out.add(memorySection(ctx))
        out.add(systemSection(ctx))
        out.add(
            StatSection(
                "Not shown",
                listOf(
                    "Exact battery capacity health, per-app battery use and network details " +
                        "need special access, so VIC does not read them."
                )
            )
        )
        return out
    }

    // ---------- advice ----------

    fun advice(ctx: Context): List<String> {
        val out = ArrayList<String>()
        val b = readBattery(ctx)
        if (b != null) {
            val t = b.tempC
            if (t != null && t >= 45.0) {
                out.add("The battery is hot (" + fmt1(t) + " °C). Stop heavy use and charging, take off the case, and let it cool in the shade.")
            } else if (t != null && t >= 40.0) {
                out.add("The battery is warm (" + fmt1(t) + " °C). Close demanding apps, remove the case, and avoid charging in the sun or while gaming.")
            }
            if (!b.healthOk) {
                out.add("Android rates the battery health as " + b.healthText + ". If that persists, have the battery checked.")
            }
            if (b.level in 0..29 && !b.charging) {
                out.add("Battery is at " + b.level + "%. Turn on Battery Saver, lower brightness, and close heavy apps until you can charge.")
            }
            if (b.charging && b.level >= 90) {
                out.add("To keep the battery healthy, avoid leaving it at 100% for hours. Unplugging around 80 to 90% when convenient helps.")
            }
            val c = b.cycles
            if (c != null && c >= 500) {
                out.add("The battery has been through " + c + " charge cycles, so reduced capacity would be normal.")
            }
        }

        val s = storageBytes()
        if (s != null && s.first > 0) {
            val freePct = (s.second * 100 / s.first).toInt()
            if (freePct < 10) {
                out.add("Storage is very low (" + freePct + "% free). Delete unused apps, clear downloads, and back up photos and videos elsewhere.")
            } else if (freePct < 20) {
                out.add("Storage is getting tight (" + freePct + "% free). Review large apps, downloads and old media soon.")
            }
        }

        if (memoryInfo(ctx).lowMemory) {
            out.add("The phone is low on memory. Close unused apps, and restart if it stays slow.")
        }

        val br = brightness(ctx)
        if (br != null && !br.second && br.first >= 80) {
            out.add("Brightness is high and manual. Lowering it or using adaptive brightness saves battery.")
        }
        if (uptimeDays() >= 7L) {
            out.add("The phone has run for " + uptimeDays() + " days without a restart. A restart now and then clears slowdowns.")
        }
        val patch = patchAgeDays()
        if (patch != null && patch > 365L) {
            out.add("The security patch is over a year old. Check for system updates.")
        }

        if (out.isEmpty()) out.add("Nothing needs attention right now, Sir.")
        return out
    }

    fun batteryTips(ctx: Context): String {
        val b = readBattery(ctx)
        val now = if (b != null && b.level >= 0) {
            "\n\nRight now the battery is at " + b.level + "%, " + b.statusText.lowercase(Locale.ROOT) + "."
        } else {
            ""
        }
        return "To save battery, Sir:\n" +
            "- Use Battery Saver when below about 30%.\n" +
            "- Lower brightness or use adaptive brightness; the screen is usually the biggest drain.\n" +
            "- Use dark mode if your screen is OLED.\n" +
            "- Restrict background activity for apps you rarely use (Settings, Apps, the app, Battery).\n" +
            "- Turn off Bluetooth and location when you do not need them.\n" +
            "- Keep the phone cool, and avoid charging it under a pillow or in the sun." + now
    }

    fun storageTips(ctx: Context): String {
        val s = storageBytes()
        val now = if (s != null) "\n\nRight now you have " + gb(s.second) + " free of " + gb(s.first) + "." else ""
        return "To free up space, Sir:\n" +
            "- Open Settings, Storage, and see what is using the most.\n" +
            "- Uninstall apps you no longer use.\n" +
            "- Clear the cache of large apps such as browsers and social apps.\n" +
            "- Back up photos and videos to cloud or another device, then delete the local copies.\n" +
            "- Empty the Downloads folder and the trash or recycle bins in your gallery and file apps." + now
    }

    // ---------- text ----------

    fun textOf(section: StatSection): String {
        val sb = StringBuilder(section.title.uppercase(Locale.ROOT))
        section.lines.forEach { sb.append("\n- ").append(it) }
        return sb.toString()
    }

    fun report(ctx: Context): String {
        val sb = StringBuilder("Here is your device report, Sir.")
        sections(ctx).forEach { sb.append("\n\n").append(textOf(it)) }
        sb.append("\n\nADVICE")
        advice(ctx).forEach { sb.append("\n- ").append(it) }
        return sb.toString()
    }
}
