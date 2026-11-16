package com.vale.vic.automation.actions

import android.content.Context
import android.content.Intent
import java.util.Locale

data class AppInfo(val label: String, val pkg: String)

sealed class FindResult {
    class Found(val app: AppInfo) : FindResult()
    object NotFound : FindResult()
    class Ambiguous(val names: List<String>) : FindResult()
}

object AppFinder {

    fun launchableApps(ctx: Context): List<AppInfo> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val list = pm.queryIntentActivities(intent, 0)
        return list
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
    }

    fun find(ctx: Context, name: String): FindResult {
        val query = name.trim().lowercase(Locale.ROOT)
        if (query.isEmpty()) return FindResult.NotFound
        val apps = launchableApps(ctx).filter { it.pkg != ctx.packageName }

        val exact = apps.filter { it.label.lowercase(Locale.ROOT) == query }
        if (exact.size == 1) return FindResult.Found(exact[0])
        if (exact.size > 1) {
            return FindResult.Ambiguous(exact.map { it.label + " (" + it.pkg + ")" }.take(5))
        }

        val partial = apps.filter { it.label.lowercase(Locale.ROOT).contains(query) }
        if (partial.size == 1) return FindResult.Found(partial[0])
        if (partial.size > 1) {
            return FindResult.Ambiguous(partial.map { it.label }.take(5))
        }
        return FindResult.NotFound
    }
}
