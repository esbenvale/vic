package com.vale.vic.automation.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ActionType {
    OPEN_APP,
    CLICK_ELEMENT,
    SCROLL,
    TYPE_TEXT,
    PRESS_BACK,
    WAIT,
    READ_SCREEN,
    COMPLETE_TASK
}

enum class ActionState {
    PENDING,
    RUNNING,
    SUCCEEDED,
    UNVERIFIED,
    FAILED,
    CANCELLED
}

data class ActionResult(val state: ActionState, val message: String)

class AutoAction(
    val id: Int,
    val type: ActionType,
    val description: String,
    val timeoutMs: Long,
    val arg: String = ""
) {
    var state by mutableStateOf(ActionState.PENDING)
    var message by mutableStateOf("")
}
