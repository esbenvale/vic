package com.vale.vic.automation.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object VicServiceState {
    var connected by mutableStateOf(false)
    var lastExternalPackage by mutableStateOf<String?>(null)
}
