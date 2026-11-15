package com.vale.vic.ai

import android.content.Context

object AiSettings {
    private const val PREFS = "vic_ai_settings"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun baseUrl(ctx: Context): String = prefs(ctx).getString("base_url", "") ?: ""
    fun model(ctx: Context): String = prefs(ctx).getString("model", "") ?: ""
    fun apiKey(ctx: Context): String = prefs(ctx).getString("api_key", "") ?: ""

    fun save(ctx: Context, baseUrl: String, model: String, apiKey: String) {
        prefs(ctx).edit()
            .putString("base_url", baseUrl.trim())
            .putString("model", model.trim())
            .putString("api_key", apiKey.trim())
            .apply()
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    /** Returns null unless all three settings are filled in. */
    fun provider(ctx: Context): AiProvider? {
        val url = baseUrl(ctx)
        val model = model(ctx)
        val key = apiKey(ctx)
        if (url.isBlank() || model.isBlank() || key.isBlank()) return null
        return OpenAiCompatibleProvider(url, model, key)
    }
}
