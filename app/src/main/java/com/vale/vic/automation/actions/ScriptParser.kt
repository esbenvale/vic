package com.vale.vic.automation.actions

import java.util.Locale

/** Turns a few simple lines into a list of actions. No guessing. */
object ScriptParser {

    class Result(val actions: List<AutoAction>, val error: String?)

    private const val MAX_LINES = 12

    fun parse(script: String, addLeadIn: Boolean): Result {
        val lines = script.split('\n', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return Result(emptyList(), "The script is empty.")
        if (lines.size > MAX_LINES) {
            return Result(emptyList(), "Too many steps (maximum " + MAX_LINES + ").")
        }

        val out = ArrayList<AutoAction>()
        var next = 1
        fun add(type: ActionType, desc: String, timeout: Long, arg: String = "") {
            out.add(AutoAction(next, type, desc, timeout, arg))
            next++
        }

        val firstIsOpen = lines[0].lowercase(Locale.ROOT).startsWith("open ")
        if (addLeadIn && !firstIsOpen) {
            add(
                ActionType.WAIT,
                "Wait 6 seconds. Switch to the app you want me to operate.",
                7000L,
                "6000"
            )
        }

        for ((i, line) in lines.withIndex()) {
            val lower = line.lowercase(Locale.ROOT)
            val missing = Result(emptyList(), "Step " + (i + 1) + " is missing its target: " + line)
            when {
                lower.startsWith("open ") -> {
                    val a = line.substring(5).trim()
                    if (a.isEmpty()) return missing
                    add(ActionType.OPEN_APP, "Open " + a, 12000L, a)
                }
                lower.startsWith("tap ") || lower.startsWith("click ") -> {
                    val a = line.substringAfter(' ').trim()
                    if (a.isEmpty()) return missing
                    add(ActionType.CLICK_ELEMENT, "Tap \"" + a + "\"", 9000L, a)
                }
                lower == "scroll" || lower == "scroll down" ->
                    add(ActionType.SCROLL, "Scroll down", 5000L, "down")
                lower == "scroll up" ->
                    add(ActionType.SCROLL, "Scroll up", 5000L, "up")
                lower == "back" || lower == "go back" || lower == "press back" ->
                    add(ActionType.PRESS_BACK, "Press Back", 4000L)
                lower.startsWith("type ") -> {
                    val a = line.substring(5)
                    if (a.isBlank()) return missing
                    add(ActionType.TYPE_TEXT, "Type text (" + a.length + " characters)", 5000L, a)
                }
                lower.startsWith("wait ") -> {
                    val secs = lower.removePrefix("wait ").trim().removeSuffix("s").trim()
                        .toDoubleOrNull()
                        ?: return Result(emptyList(), "Step " + (i + 1) + " needs a number: " + line)
                    val clamped = if (secs < 0.5) 0.5 else if (secs > 30.0) 30.0 else secs
                    val ms = (clamped * 1000).toLong()
                    add(ActionType.WAIT, "Wait " + clamped + " seconds", ms + 1000L, ms.toString())
                }
                lower == "read" || lower == "read screen" ->
                    add(ActionType.READ_SCREEN, "Read the screen", 5000L)
                else ->
                    return Result(emptyList(), "Step " + (i + 1) + " not understood: " + line)
            }
        }
        add(ActionType.COMPLETE_TASK, "Task complete", 1000L)
        return Result(out, null)
    }
}
