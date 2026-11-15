
package com.vale.vic

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.permissions.PermissionRegistry
import com.vale.vic.ui.theme.VicColors
import com.vale.vic.ui.theme.VicTheme

class MainActivity : ComponentActivity() {

    private val accessibilityEnabled = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accessibilityEnabled.value =
            PermissionRegistry.isAccessibilityEnabled(this)

        setContent {
            VicTheme {
                VicHome(
                    accessibilityEnabled = accessibilityEnabled.value,
                    onOpenAccessibilitySettings = {
                        PermissionRegistry.openAccessibilitySettings(this)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        accessibilityEnabled.value =
            PermissionRegistry.isAccessibilityEnabled(this)
    }
}

@Composable
fun VicHome(
    accessibilityEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VicColors.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "VIC",
            color = VicColors.Cyan,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "VALE INTELLIGENT COMPANION",
            color = VicColors.TextMuted,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = VicColors.Panel
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SYSTEM STATUS",
                    color = VicColors.Cyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                SystemInfo(
                    "Device",
                    "${Build.MANUFACTURER} ${Build.MODEL}"
                )
                SystemInfo("Android", Build.VERSION.RELEASE)
                SystemInfo("API level", Build.VERSION.SDK_INT.toString())
                SystemInfo("Application", "VIC 1.0")
                SystemInfo("Core", "Initialisation complete")

                SystemInfo(
                    "Accessibility",
                    if (accessibilityEnabled) "Enabled" else "Not enabled"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = VicColors.Blue
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "OPEN ACCESSIBILITY SETTINGS",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "CORE SYSTEM",
            color = VicColors.Cyan,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (accessibilityEnabled) {
                "VIC accessibility service is connected."
            } else {
                "VIC is ready. Enable its accessibility service through Android Settings when you choose to."
            },
            color = VicColors.TextMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "VALE CORPORATIONS",
            color = VicColors.TextMuted,
            fontSize = 10.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun SystemInfo(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = VicColors.TextMuted,
            fontSize = 13.sp
        )

        Text(
            text = value,
            color = VicColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
