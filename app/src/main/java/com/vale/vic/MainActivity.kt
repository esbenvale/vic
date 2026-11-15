package com.vale.vic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.ai.ChatStore
import com.vale.vic.automation.service.VicServiceState
import com.vale.vic.ui.AiSettingsScreen
import com.vale.vic.ui.ChatScreen
import com.vale.vic.ui.SetupScreen
import com.vale.vic.ui.theme.VicColors
import com.vale.vic.ui.theme.VicTheme

class MainActivity : ComponentActivity() {

    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChatStore.ensureGreeting()
        setContent {
            VicTheme {
                VicApp(refreshTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }
}

private enum class Screen(val label: String) {
    CHAT("Chat"),
    AI("AI"),
    SETUP("Setup")
}

@Composable
private fun VicApp(refreshTick: Int) {
    var screen by remember { mutableStateOf(Screen.CHAT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VicColors.Charcoal)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VIC",
                color = VicColors.Cyan,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp
            )
            Text(
                text = if (VicServiceState.connected) "ACCESSIBILITY ONLINE" else "ACCESSIBILITY OFF",
                color = if (VicServiceState.connected) VicColors.Cyan else VicColors.TextMuted,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Screen.values().forEach { s ->
                TextButton(onClick = { screen = s }) {
                    Text(
                        text = s.label.uppercase(),
                        color = if (screen == s) VicColors.Cyan else VicColors.TextMuted,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (screen) {
                Screen.CHAT -> ChatScreen()
                Screen.AI -> AiSettingsScreen()
                Screen.SETUP -> SetupScreen(refreshTick)
            }
        }
    }
}
