package com.vale.vic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.ai.AiSettings
import com.vale.vic.ui.theme.VicColors

@Composable
fun AiSettingsScreen() {
    val context = LocalContext.current
    var baseUrl by remember { mutableStateOf(AiSettings.baseUrl(context)) }
    var model by remember { mutableStateOf(AiSettings.model(context)) }
    var apiKey by remember { mutableStateOf(AiSettings.apiKey(context)) }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Panel {
            SectionTitle("AI provider")
            Text(
                text = "Optional. Without this, VIC still answers its built-in offline " +
                    "commands. Once filled in, your messages and recent chat are sent to " +
                    "the provider you choose. The key stays on this phone in the app's " +
                    "private storage (not yet encrypted).",
                color = VicColors.TextPrimary,
                fontSize = 14.sp
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it; status = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL (https://...)") },
                singleLine = true
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it; status = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model name") },
                singleLine = true
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; status = "" },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Button(
                onClick = {
                    if (!baseUrl.trim().startsWith("https://")) {
                        status = "The base URL must start with https://"
                    } else {
                        AiSettings.save(context, baseUrl, model, apiKey)
                        status = "Saved."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VicColors.Cyan,
                    contentColor = VicColors.Black
                )
            ) {
                Text("Save")
            }
            OutlinedButton(
                onClick = {
                    AiSettings.clear(context)
                    baseUrl = ""
                    model = ""
                    apiKey = ""
                    status = "Cleared. VIC is back to offline commands only."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear provider", color = VicColors.Cyan)
            }
            if (status.isNotEmpty()) {
                Text(text = status, color = VicColors.TextMuted, fontSize = 13.sp)
            }
        }

        Panel {
            SectionTitle("Where to get a key")
            Text(
                text = "Any provider with an OpenAI-compatible chat endpoint works. " +
                    "Examples of base URLs:\n\n" +
                    "https://generativelanguage.googleapis.com/v1beta/openai\n" +
                    "https://api.groq.com/openai/v1\n" +
                    "https://openrouter.ai/api/v1\n\n" +
                    "Create a key in the provider's own dashboard, then copy a model name " +
                    "from its model list. Free tiers, limits and model names change, so " +
                    "check the provider's current terms.",
                color = VicColors.TextPrimary,
                fontSize = 14.sp
            )
        }
    }
}
