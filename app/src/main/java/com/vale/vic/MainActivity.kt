package com.vale.vic

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.automation.permissions.PermissionRegistry
import com.vale.vic.automation.service.VicServiceState
import com.vale.vic.ui.theme.VicColors
import com.vale.vic.ui.theme.VicTheme

class MainActivity : ComponentActivity() {

    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VicTheme {
                VicHome(refreshTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }
}

@Composable
private fun VicHome(refreshTick: Int) {
    val context = LocalContext.current
    val accessibility = remember(refreshTick) { PermissionRegistry.accessibility(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VicColors.Charcoal)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "VIC",
            color = VicColors.Cyan,
            fontSize = 44.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 10.sp
        )
        Text(
            text = "VALE INTELLIGENT COMPANION  |  AUTO WORK SETUP",
            color = VicColors.TextMuted,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Panel {
            Text(
                text = accessibility.title.uppercase(),
                color = VicColors.Cyan,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )
            Text(text = accessibility.why, color = VicColors.TextPrimary, fontSize = 14.sp)
            StatusRow("Enabled in Android settings", accessibility.enabledInAndroid)
            StatusRow("VIC service connected", VicServiceState.connected)
            InfoRow("Last other app seen", VicServiceState.lastExternalPackage ?: "none yet")

            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VicColors.Cyan,
                    contentColor = VicColors.Black
                )
            ) {
                Text("Open Accessibility settings")
            }
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + context.packageName)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open VIC app info", color = VicColors.Cyan)
            }
        }

        Panel {
            Text(
                text = "HOW TO ENABLE",
                color = VicColors.Cyan,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "1. Tap Open Accessibility settings, then find VIC Automation " +
                    "(often under Installed apps or Downloaded apps).\n" +
                    "2. If the switch is greyed out or says Restricted setting: " +
                    "tap Open VIC app info, open the three-dot menu at the top right, " +
                    "choose Allow restricted settings, and confirm. Then repeat step 1.\n" +
                    "3. Turn it on and accept Android's confirmation.\n" +
                    "4. Come back to VIC. Both statuses above should read Yes.",
                color = VicColors.TextPrimary,
                fontSize = 14.sp
            )
        }

        Panel {
            Text(
                text = "DEVICE",
                color = VicColors.Cyan,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )
            InfoRow("Build", BuildConfig.VERSION_NAME + " (#" + BuildConfig.VERSION_CODE + ")")
            InfoRow("Device", Build.MANUFACTURER + " " + Build.MODEL)
            InfoRow("Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VicColors.Panel, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = VicColors.TextMuted, fontSize = 14.sp)
        Text(
            text = if (ok) "Yes" else "No",
            color = if (ok) VicColors.Cyan else Color(0xFFFF6B6B),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = VicColors.TextMuted, fontSize = 14.sp)
        Text(text = value, color = VicColors.TextPrimary, fontSize = 14.sp)
    }
}
