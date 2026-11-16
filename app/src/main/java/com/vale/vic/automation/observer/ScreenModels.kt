package com.vale.vic.automation.observer

data class UiElement(
    val label: String,
    val kind: String,
    val viewId: String,
    val clickable: Boolean,
    val editable: Boolean,
    val scrollable: Boolean,
    val checkable: Boolean,
    val checked: Boolean,
    val enabled: Boolean,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val depth: Int
)

data class ScreenObservation(
    val ok: Boolean,
    val message: String,
    val packageName: String,
    val elements: List<UiElement>,
    val truncated: Boolean,
    val takenAt: Long
)
