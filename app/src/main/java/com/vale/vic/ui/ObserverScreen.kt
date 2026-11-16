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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.automation.observer.ObserverState
import com.vale.vic.automation.observer.UiElement
import com.vale.vic.ui.theme.VicColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SHOW_LIMIT = 80

@Composable
fun ObserverScreen() {
    val context = LocalContext.current
    val obs = ObserverState.latest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Panel {
            SectionTitle("Screen observer")
            Text(
                text = "Reads the labels, buttons, switches and lists of the active " +
                    "window. Password fields and text-field contents are never read. " +
                    "The reading stays in memory only; nothing is saved or sent anywhere.",
                color = VicColors.TextPrimary,
                fontSize = 14.sp
            )
            Button(
                onClick = { ObserverState.captureNow() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VicColors.Cyan,
                    contentColor = VicColors.Black
                )
            ) {
                Text("Read this screen now")
            }
            OutlinedButton(
                onClick = { ObserverState.captureDelayed(context, 6000L) },
                enabled = !ObserverState.waiting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (ObserverState.waiting) {
                        "Switch to the other app now..."
                    } else {
                        "Read the other app in 6 seconds"
                    },
                    color = VicColors.Cyan
                )
            }
            Text(
                text = "For the second button: tap it, press Home, open the app you " +
                    "want read, and wait. A message appears when VIC has read it. " +
                    "Then return to VIC to see the result.",
                color = VicColors.TextMuted,
                fontSize = 12.sp
            )
        }

        if (obs != null) {
            Panel {
                SectionTitle("Latest reading")
                if (!obs.ok) {
                    Text(text = obs.message, color = VicColors.TextPrimary, fontSize = 14.sp)
                } else {
                    InfoRow("App", obs.packageName)
                    InfoRow(
                        "Time",
                        SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(obs.takenAt))
                    )
                    InfoRow("Elements", obs.elements.size.toString())
                    InfoRow("Tappable", obs.elements.count { it.clickable }.toString())
                    InfoRow("Text fields", obs.elements.count { it.editable }.toString())
                    InfoRow("Scrollable", obs.elements.count { it.scrollable }.toString())
                    if (obs.truncated) {
                        Text(
                            text = "This screen is large, so only the first part was read.",
                            color = VicColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (obs.ok) {
                Panel {
                    SectionTitle("Elements")
                    if (obs.elements.size > SHOW_LIMIT) {
                        Text(
                            text = "Showing the first " + SHOW_LIMIT + " of " + obs.elements.size + ".",
                            color = VicColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    obs.elements.take(SHOW_LIMIT).forEach { e ->
                        Text(
                            text = describe(e),
                            color = VicColors.TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private fun describe(e: UiElement): String {
    val sb = StringBuilder()
    sb.append(e.kind)
    if (e.label.isEmpty()) {
        sb.append(" (no label)")
    } else {
        sb.append(" \"").append(e.label).append("\"")
    }
    val flags = ArrayList<String>()
    if (e.clickable) flags.add("tap")
    if (e.scrollable) flags.add("scroll")
    if (e.checkable) flags.add(if (e.checked) "on" else "off")
    if (!e.enabled) flags.add("disabled")
    if (flags.isNotEmpty()) {
        sb.append("  [").append(flags.joinToString(", ")).append("]")
    }
    sb.append("\n   at ")
        .append(e.left).append(",").append(e.top)
        .append(" to ")
        .append(e.right).append(",").append(e.bottom)
    if (e.viewId.isNotEmpty()) {
        sb.append("  id=").append(e.viewId)
    }
    return sb.toString()
}
