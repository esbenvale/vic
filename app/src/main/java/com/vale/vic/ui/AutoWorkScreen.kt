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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.automation.actions.ActionExecutor
import com.vale.vic.automation.actions.ActionState
import com.vale.vic.automation.actions.ScriptParser
import com.vale.vic.automation.service.VicAccessibilityService
import com.vale.vic.ui.theme.VicColors

private fun colorFor(state: ActionState): Color = when (state) {
    ActionState.SUCCEEDED -> VicColors.Cyan
    ActionState.RUNNING -> VicColors.Blue
    ActionState.UNVERIFIED -> Color(0xFFFFC857)
    ActionState.FAILED -> Color(0xFFFF6B6B)
    ActionState.PENDING -> VicColors.TextMuted
    ActionState.CANCELLED -> VicColors.TextMuted
}

@Composable
fun AutoWorkScreen() {
    val context = LocalContext.current
    var script by remember { mutableStateOf("open Settings\nwait 2\nscroll down\nread") }
    var error by remember { mutableStateOf("") }

    fun run(title: String, text: String, leadIn: Boolean) {
        if (VicAccessibilityService.instance == null) {
            error = "My accessibility service is not connected. Check the Setup tab."
            return
        }
        val parsed = ScriptParser.parse(text, leadIn)
        val err = parsed.error
        if (err != null) {
            error = err
            return
        }
        error = if (ActionExecutor.start(context, title, parsed.actions)) {
            ""
        } else {
            "A task is already running. Stop it first."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Panel {
            SectionTitle("Auto Work")
            Text(
                text = if (ActionExecutor.running) "RUNNING" else "IDLE",
                color = if (ActionExecutor.running) VicColors.Cyan else VicColors.TextMuted,
                fontSize = 14.sp
            )
            if (ActionExecutor.title.isNotEmpty()) {
                Text(text = "Task: " + ActionExecutor.title, color = VicColors.TextPrimary, fontSize = 14.sp)
            }
            ActionExecutor.steps.forEach { s ->
                Text(
                    text = "[" + s.id + "] " + s.description + " - " + s.state.name,
                    color = colorFor(s.state),
                    fontSize = 13.sp
                )
                if (s.message.isNotEmpty()) {
                    Text(text = "     " + s.message, color = VicColors.TextMuted, fontSize = 12.sp)
                }
            }
            OutlinedButton(
                onClick = { ActionExecutor.stop("Stopped from the Auto Work tab.") },
                enabled = ActionExecutor.running,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("STOP", color = VicColors.Cyan)
            }
        }

        Panel {
            SectionTitle("Quick demos")
            Button(
                onClick = { run("Open Settings and read it", "open Settings\nwait 2\nread", false) },
                enabled = !ActionExecutor.running,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VicColors.Cyan,
                    contentColor = VicColors.Black
                )
            ) {
                Text("Demo: open Settings and read it")
            }
            Button(
                onClick = {
                    run(
                        "Open Settings and scroll",
                        "open Settings\nwait 2\nscroll down\nwait 1\nscroll up\nread",
                        false
                    )
                },
                enabled = !ActionExecutor.running,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VicColors.Cyan,
                    contentColor = VicColors.Black
                )
            ) {
                Text("Demo: open Settings and scroll")
            }
        }

        Panel {
            SectionTitle("Script")
            Text(
                text = "One step per line (or separated by ;): open <app>, tap <text>, " +
                    "scroll down, scroll up, back, type <text>, wait <seconds>, read. " +
                    "If the first step is not open, I wait 6 seconds so you can switch " +
                    "to the right app.",
                color = VicColors.TextPrimary,
                fontSize = 13.sp
            )
            OutlinedTextField(
                value = script,
                onValueChange = { script = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Script") },
                minLines = 3
            )
            Button(
                onClick = { run("Custom script", script, true) },
                enabled = !ActionExecutor.running,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VicColors.Cyan,
                    contentColor = VicColors.Black
                )
            ) {
                Text("Run script")
            }
            if (error.isNotEmpty()) {
                Text(text = error, color = Color(0xFFFF6B6B), fontSize = 13.sp)
            }
        }

        Panel {
            SectionTitle("Safety")
            Text(
                text = "A run stops automatically if you open VIC while it is working. " +
                    "I never type into or tap password fields, and I refuse targets such " +
                    "as pay, buy, send, delete, install and reset until approval support " +
                    "exists. A floating STOP button and notification are coming next."
