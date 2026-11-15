package com.vale.vic.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val source: String,
    val id: Long = System.nanoTime()
)

object ChatStore {
    val messages = mutableStateListOf<ChatMessage>()
    var busy by mutableStateOf(false)

    fun ensureGreeting() {
        if (messages.isEmpty()) {
            messages.add(
                ChatMessage(
                    fromUser = false,
                    text = "At your service, Sir. Type \"help\" to see what I can do right now.",
                    source = "local"
                )
            )
        }
    }

    fun clear() {
        messages.clear()
        ensureGreeting()
    }
}
