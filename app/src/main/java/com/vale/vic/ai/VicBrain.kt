package com.vale.vic.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/** Routes a message: built-in commands first, then the configured AI provider. */
object VicBrain {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    /** Call from the main thread. */
    fun submit(context: Context, input: String) {
        val text = input.trim()
        if (text.isEmpty() || ChatStore.busy) return
        val app = context.applicationContext

        ChatStore.messages.add(ChatMessage(fromUser = true, text = text, source = "you"))
        ChatStore.busy = true
        val history = ChatStore.messages.toList()

        executor.execute {
            val reply = respond(app, text, history)
            mainHandler.post {
                ChatStore.messages.add(reply)
                ChatStore.busy = false
            }
        }
    }

    private fun respond(app: Context, text: String, history: List<ChatMessage>): ChatMessage {
        val local = LocalEngine.handle(app, text)
        if (local != null) {
            return ChatMessage(fromUser = false, text = local, source = "local")
        }
        val provider = AiSettings.provider(app)
            ?: return ChatMessage(fromUser = false, text = LocalEngine.noAiNotice(), source = "local")

        val result = provider.reply(history)
        return if (result.text != null) {
            ChatMessage(fromUser = false, text = result.text, source = "ai")
        } else {
            ChatMessage(
                fromUser = false,
                text = "The AI request failed: " + (result.error ?: "unknown error"),
                source = "system"
            )
        }
    }
}
