package com.vale.vic.ai

import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import org.json.JSONArray
import org.json.JSONObject

data class AiResult(val text: String?, val error: String?)

interface AiProvider {
    val name: String
    fun reply(history: List<ChatMessage>): AiResult
}

private const val SYSTEM_PROMPT =
    "You are VIC, the VALE Intelligent Companion, a composed and professional " +
        "personal assistant for VALE Corporations. Address the user as Sir when " +
        "natural, use dry humour only occasionally, and be concise. " +
        "IMPORTANT: in this version you can only chat. You cannot yet open apps, " +
        "tap, read the screen, send messages or change phone settings. Never claim " +
        "to have done any phone action, and say plainly that you cannot when asked. " +
        "If you are unsure of something, say so instead of guessing."

class OpenAiCompatibleProvider(
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String
) : AiProvider {

    override val name: String = "OpenAI-compatible ($model)"

    override fun reply(history: List<ChatMessage>): AiResult {
        if (!baseUrl.startsWith("https://")) {
            return AiResult(null, "The base URL must start with https://")
        }
        var conn: HttpURLConnection? = null
        return try {
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            history
                .filter { it.source != "system" }
                .takeLast(12)
                .dropWhile { !it.fromUser }
                .forEach { m ->
                    val role = if (m.fromUser) "user" else "assistant"
                    messages.put(JSONObject().put("role", role).put("content", m.text))
                }
            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .toString()

            val c = URL(baseUrl.trimEnd('/') + "/chat/completions")
                .openConnection() as HttpURLConnection
            conn = c
            c.requestMethod = "POST"
            c.connectTimeout = 15000
            c.readTimeout = 45000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("Authorization", "Bearer " + apiKey)
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                AiResult(null, "HTTP " + code + ": " + errorText(raw))
            } else {
                val content = JSONObject(raw)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                AiResult(content.trim(), null)
            }
        } catch (e: UnknownHostException) {
            AiResult(null, "No internet connection, or the host name is wrong.")
        } catch (e: SocketTimeoutException) {
            AiResult(null, "The request timed out.")
        } catch (e: Exception) {
            AiResult(null, e.message ?: e.javaClass.simpleName)
        } finally {
            conn?.disconnect()
        }
    }

    private fun errorText(raw: String): String {
        return try {
            JSONObject(raw).optJSONObject("error")?.optString("message").orEmpty()
                .ifBlank { raw.take(200) }
        } catch (e: Exception) {
            raw.take(200)
        }
    }
}
