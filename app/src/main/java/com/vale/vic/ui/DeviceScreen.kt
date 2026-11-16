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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.device.DeviceStats
import com.vale.vic.ui.theme.VicColors

@Composable
fun DeviceScreen(refreshTick: Int) {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val sections = remember(refreshTick, version) { DeviceStats.sections(context) }
    val advice = remember(refreshTick, version) { DeviceStats.advice(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Panel {
            SectionTitle("Device")
            Text(
                text = "A read-only view of your phone. Nothing here changes any setting.",
                color = VicColors.TextPrimary,
                fontSize = 14.sp
            )
            Button(
                onClick = { version++ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VicColors.Cyan,
                    contentColor = VicColors.Black
                )
            ) {
                Text("Refresh")
            }
        }

        sections.forEach { s ->
            Panel {
                SectionTitle(s.title)
                s.lines.forEach { line ->
                    Text(text = line, color = VicColors.TextPrimary, fontSize = 14.sp)
                }
            }
        }

        Panel {
            SectionTitle("Advice")
            advice.forEach { line ->
                Text(text = "- " + line, color = VicColors.TextPrimary, fontSize = 14.sp)
            }
        }
    }
}
