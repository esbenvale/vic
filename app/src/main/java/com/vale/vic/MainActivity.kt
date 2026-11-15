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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vale.vic.ui.theme.VicTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VicTheme {
                VicHome()
            }
        }
    }
}

@Composable
fun VicHome() {
    val background = Color(0xFF080B12)
    val blue = Color(0xFF36A9FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "VIC",
            color = blue,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "VALE INTELLIGENT COMPANION",
            color = Color.LightGray,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111827)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SYSTEM STATUS",
                    color = blue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                SystemInfo("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
                SystemInfo("Android", Build.VERSION.RELEASE)
                SystemInfo("API level", Build.VERSION.SDK_INT.toString())
                SystemInfo("Application", "VIC 1.0")
                SystemInfo("Status", "Initialisation complete")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CORE SYSTEM",
            color = blue,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "VIC is installed and ready for the next development stage.",
            color = Color.LightGray,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "VALE CORPORATIONS",
            color = Color.Gray,
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
            color = Color.Gray,
            fontSize = 13.sp
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
