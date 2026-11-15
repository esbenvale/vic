package com.vale.vic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.ai.ChatMessage
import com.vale.vic.ai.ChatStore
import com.vale.vic.ai.VicBrain
import com.vale.vic.ui.theme.VicColors

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages = ChatStore.messages
    val busy = ChatStore.busy
    val total = messages.size + (if (busy) 1 else 0)

    LaunchedEffect(total) {
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { m ->
                Bubble(m)
            }
            if (busy) {
                item {
                    Text(
                        text = "VIC is thinking...",
                        color = VicColors.TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message VIC") },
                maxLines = 4
            )
            Button(
                onClick = {
                    VicBrain.submit(context, input)
                    input = ""
                },
                enabled = input.isNotBlank() && !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VicColors.Cyan,
                    contentColor = VicColors.Black
                )
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    val mine = m.fromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (mine) Color(0xFF12343B) else VicColors.Panel,
                    RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
        ) {
            Text(text = m.text, color = VicColors.TextPrimary, fontSize = 15.sp)
            if (!mine) {
                val label = when (m.source) {
                    "ai" -> "AI"
                    "local" -> "built-in"
                    else -> "notice"
                }
                Text(
                    text = label,
                    color = VicColors.TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
