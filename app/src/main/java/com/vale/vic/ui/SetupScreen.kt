package com.vale.vic.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.automation.permissions.PermissionRegistry
import com.vale.vic.automation.service.VicServiceState
import com.vale.vic.ui.theme.VicColors

@Composable
fun SetupScreen(refreshTick: Int) {
    val context = LocalContext.current
    val accessibility = remember(refreshTick) { PermissionRegistry.accessibility(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Panel {
            SectionTitle(accessibility.title)
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
            SectionTitle("How to enable")
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
            SectionTitle("Device")
            InfoRow("App", "VIC")
            InfoRow("Device", Build.MANUFACTURER + " " + Build.MODEL)
            InfoRow("Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
        }
    }
}
