package com.vale.vic.automation.servicd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object VicServiceState {
    var isServiceConnected by mutableStateOf(false)
        private set

    fun setConnected(connected: Boolean) {
        isServiceConnected = connected
    }
}
