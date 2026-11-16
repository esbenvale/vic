package com.vale.vic.automation.actions

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vale.vic.automation.observer.ObserverState
import com.vale.vic.automation.observer.ScreenObserver
import com.vale.vic.automation.service.VicAccessibilityService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs actions one at a time on the real screen, checks the result of each,
 * and never reports success it has not seen.
 */
object ActionExecutor {

    var title by mutableStateOf("")
    var running by mutableStateOf(false)
    val steps = mutableStateListOf<AutoAction>()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)

    @Volatile
    private var cancelRequested = false

    @Volatile
    private var cancelReason = "Stopped."

    @Volatile
    private var sawOtherApp = false

    private val blocked = Regex(
        "\\b(pay|buy|purchase|order|send|delete|erase|transfer|uninstall|reset|install|format|wipe|factory)\\b",
        RegexOption.IGNORE_CASE
    )

    private enum class Kind { CLICKED, NOT_FOUND, AMBIGUOUS, REJECTED, BLOCKED }

    private class Outcome(val kind: Kind, val message: String)

    /** Safe to call from any thread. Returns false if a task is already running. */
    fun start(context: Context, planTitle: String, plan: List<AutoAction>): Boolean {
        if (plan.isEmpty()) return false
        if (!busy.compareAndSet(false, true)) return false
        cancelRequested = false
        cancelReason = "Stopped."
        sawOtherApp = false
        val app = context.applicationContext
        mainHandler.post {
            title = planTitle
            steps.clear()
            steps.addAll(plan)
            running = true
        }
        executor.execute {
            try {
                runPlan(app, plan)
            } finally {
                busy.set(false)
                mainHandler.post { running = false }
            }
        }
        return true
    }

    fun stop(reason: String) {
        if (!busy.get()) return
        cancelReason = reason
        cancelRequested = true
    }

    /** Called by the accessibility service whenever another window comes to the front. */
    fun onForegroundChanged(pkg: String, vicPackage: String) {
        if (!busy.get()) return
        if (pkg == vicPackage) {
            if (sawOtherApp) stop("VIC came to the front, so I stopped.")
        } else {
            sawOtherApp = true
        }
    }

    private fun runPlan(app: Context, plan: List<AutoAction>) {
        var stopped = false
        for (step in plan) {
            if (stopped || cancelRequested) {
                val why = if (cancelRequested) cancelReason else "Skipped because an earlier step failed."
                update(step, ActionResult(ActionState.CANCELLED, why))
                continue
            }
            update(step, ActionResult(ActionState.RUNNING, ""))
            val result = try {
                runStep(app, step)
            } catch (e: Exception) {
                ActionResult(
                    ActionState.FAILED,
                    "Unexpected error: " + (e.message ?: e.javaClass.simpleName)
                )
            }
            update(step, result)
            if (result.state == ActionState.FAILED || result.state == ActionState.CANCELLED) {
                stopped = true
            }
        }
    }

    private fun update(step: AutoAction, result: ActionResult) {
        mainHandler.post {
            step.state = result.state
            step.message = result.message
        }
    }

    private fun runStep(app: Context, step: AutoAction): ActionResult = when (step.type) {
        ActionType.OPEN_APP -> openApp(app, step)
        ActionType.CLICK_ELEMENT -> click(step)
        ActionType.SCROLL -> scroll(step)
        ActionType.TYPE_TEXT -> typeText(step)
        ActionType.PRESS_BACK -> pressBack()
        ActionType.WAIT -> waitStep(step)
        ActionType.READ_SCREEN -> readScreen()
        ActionType.COMPLETE_TASK -> ActionResult(ActionState.SUCCEEDED, "Task complete.")
    }

    // ---------- helpers ----------

    private fun service(): VicAccessibilityService? = VicAccessibilityService.instance

    private fun fail(message: String) = ActionResult(ActionState.FAILED, message)

    private fun cancelledResult() = ActionResult(ActionState.CANCELLED, cancelReason)

    private fun sleepChecked(ms: Long): Boolean {
        var left = ms
        while (left > 0) {
            if (cancelRequested) return false
            val chunk = if (left > 100L) 100L else left
            try {
                Thread.sleep(chunk)
            } catch (e: InterruptedException) {
                return false
            }
            left -= chunk
        }
        return !cancelRequested
    }

    @Suppress("DEPRECATION")
    private fun recycle(node: AccessibilityNodeInfo?) {
        try {
            node?.recycle()
        } catch (e: Exception) {
            // No-op on newer Android versions.
        }
    }

    private fun signature(): String {
        val obs = ScreenObserver.observe()
        if (!obs.ok) return "none"
        return obs.packageName + "|" + obs.elements.size + "|" +
            obs.elements.joinToString("\u0001") { it.label }.hashCode()
    }

    /** 1 = changed, 0 = unchanged, -1 = cancelled. */
    private fun waitForChange(before: String, timeoutMs: Long): Int {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!sleepChecked(400L)) return -1
            if (signature() != before) return 1
        }
        return 0
    }

    private fun verify(before: String, timeoutMs: Long, done: String): ActionResult =
        when (waitForChange(before, timeoutMs)) {
            1 -> ActionResult(ActionState.SUCCEEDED, done + " The screen changed.")
            -1 -> cancelledResult()
            else -> ActionResult(
                ActionState.UNVERIFIED,
                done + " Android accepted it, but I saw no visible change."
            )
        }

    private fun frontPackage(svc: VicAccessibilityService): String? {
        val root = svc.rootInActiveWindow ?: return null
        val p = root.packageName?.toString()
        recycle(root)
        return p
    }

    private fun labelOf(n: AccessibilityNodeInfo): String =
        (n.text?.toString() ?: n.contentDescription?.toString() ?: "").trim()

    // ---------- actions ----------

    private fun openApp(app: Context, step: AutoAction): ActionResult {
        val svc = service() ?: return fail("My accessibility service is not connected.")
        val found = AppFinder.find(app, step.arg)
        val target: AppInfo = when (found) {
            is FindResult.Found -> found.app
            FindResult.NotFound -> return fail("No installed app matches \"" + step.arg + "\".")
            is FindResult.Ambiguous -> return fail(
                "\"" + step.arg + "\" matches several apps: " +
                    found.names.joinToString(", ") + ". Please be more specific."
            )
        }
        val intent = app.packageManager.getLaunchIntentForPackage(target.pkg)
            ?: return fail(target.label + " has no launchable screen.")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            svc.startActivity(intent)
        } catch (e: Exception) {
            return fail("Android refused to open " + target.label + ": " + (e.message ?: "unknown reason"))
        }
        val deadline = System.currentTimeMillis() + step.timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!sleepChecked(300L)) return cancelledResult()
            if (frontPackage(svc) == target.pkg) {
                return ActionResult(ActionState.SUCCEEDED, "Opened " + target.label + ".")
            }
        }
        return fail(target.label + " did not come to the front within " + (step.timeoutMs / 1000) + " seconds.")
    }

    private fun click(step: AutoAction): ActionResult {
        val svc = service() ?: return fail("My accessibility service is not connected.")
        val target = step.arg.trim()
        if (target.isEmpty()) return fail("No target text was given.")
        if (blocked.containsMatchIn(target)) {
            return fail(
                "\"" + target + "\" looks like a sensitive action, and I cannot ask for " +
                    "your approval yet. Please tap it yourself."
            )
        }
        val before = signature()
        val deadline = System.currentTimeMillis() + step.timeoutMs
        var last = Outcome(Kind.NOT_FOUND, "I could not find \"" + target + "\" on the screen.")
        while (System.currentTimeMillis() < deadline) {
            val root = svc.rootInActiveWindow
            if (root != null) {
                last = tryClick(root, target)
                recycle(root)
                if (last.kind == Kind.CLICKED) break
                if (last.kind != Kind.NOT_FOUND) return fail(last.message)
            }
            if (!sleepChecked(500L)) return cancelledResult()
        }
        if (last.kind != Kind.CLICKED) return fail(last.message)
        return verify(before, 3000L, last.message)
    }

    private fun tryClick(root: AccessibilityNodeInfo, target: String): Outcome {
        val found: List<AccessibilityNodeInfo> =
            root.findAccessibilityNodeInfosByText(target) ?: emptyList()
        val visible = found.filter { it.isVisibleToUser }
        val exact = visible.filter { labelOf(it).equals(target, ignoreCase = true) }
        val pool = if (exact.isNotEmpty()) exact else visible

        val seen = HashSet<String>()
        val targets = ArrayList<AccessibilityNodeInfo>()
        for (n in pool) {
            val c = clickableAncestor(n) ?: continue
            val r = Rect()
            c.getBoundsInScreen(r)
            if (seen.add(r.flattenToString())) targets.add(c)
        }
        if (targets.isEmpty()) {
            return Outcome(Kind.NOT_FOUND, "I could not find a tappable \"" + target + "\" on the screen.")
        }
        if (targets.size > 1) {
            return Outcome(
                Kind.AMBIGUOUS,
                "\"" + target + "\" matches " + targets.size + " tappable items, so I did not guess."
            )
        }
        val node = targets[0]
        if (node.isPassword) {
            return Outcome(Kind.BLOCKED, "That is a password field, so I will not touch it.")
        }
        val name = labelOf(node).ifEmpty { target }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return if (ok) {
            Outcome(Kind.CLICKED, "Tapped \"" + name + "\".")
        } else {
            Outcome(Kind.REJECTED, "Android rejected the tap on \"" + name + "\".")
        }
    }

    private fun clickableAncestor(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var cur: AccessibilityNodeInfo? = n
        var depth = 0
        while (cur != null && depth < 8) {
            if (cur.isClickable && cur.isEnabled) return cur
            cur = cur.parent
            depth++
        }
        return null
    }

    private fun scroll(step: AutoAction): ActionResult {
        val svc = service() ?: return fail("My accessibility service is not connected.")
        val down = !step.arg.equals("up", ignoreCase = true)
        val before = signature()
        val root = svc.rootInActiveWindow ?: return fail("There is no readable window to scroll.")
        val scrollable = largestScrollable(root)
        if (scrollable == null) {
            recycle(root)
            return fail("I could not find anything scrollable on this screen.")
        }
        val action = if (down) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        val ok = scrollable.performAction(action)
        recycle(root)
        if (!ok) return fail("Android rejected the scroll. I may already be at the end.")
        return verify(before, 2500L, if (down) "Scrolled down." else "Scrolled up.")
    }

    private fun largestScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestArea = -1L
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var visited = 0
        while (stack.isNotEmpty() && visited < 400) {
            val n = stack.removeLast()
            visited++
            if (n.isScrollable && n.isVisibleToUser) {
                val r = Rect()
                n.getBoundsInScreen(r)
                val area = r.width().toLong() * r.height().toLong()
                if (area > bestArea) {
                    bestArea = area
                    best = n
                }
            }
            for (i in 0 until n.childCount) {
                val c = n.getChild(i)
                if (c != null) stack.addLast(c)
            }
        }
        return best
    }

    private fun firstEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var visited = 0
        while (stack.isNotEmpty() && visited < 400) {
            val n = stack.removeLast()
            visited++
            if (n.isEditable && n.isVisibleToUser) return n
            for (i in 0 until n.childCount) {
                val c = n.getChild(i)
                if (c != null) stack.addLast(c)
            }
        }
        return null
    }

    private fun typeText(step: AutoAction): ActionResult {
        val svc = service() ?: return fail("My accessibility service is not connected.")
        val root = svc.rootInActiveWindow ?: return fail("There is no readable window.")
        var node: AccessibilityNodeInfo? = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (node == null || !node.isEditable) node = firstEditable(root)
        if (node == null) {
            recycle(root)
            return fail("I could not find a text field on this screen.")
        }
        if (node.isPassword) {
            recycle(root)
            return fail("That is a password field, so I will not type into it.")
        }
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, step.arg)
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!ok) {
            recycle(root)
            return fail("Android rejected the text entry.")
        }
        node.refresh()
        val typed = node.text?.toString() ?: ""
        recycle(root)
        return if (typed.contains(step.arg)) {
            ActionResult(ActionState.SUCCEEDED, "Typed " + step.arg.length + " characters.")
        } else {
            ActionResult(
                ActionState.UNVERIFIED,
                "Android accepted the text, but I could not confirm it appeared."
            )
        }
    }

    private fun pressBack(): ActionResult {
        val svc = service() ?: return fail("My accessibility service is not connected.")
        val before = signature()
        if (!svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)) {
            return fail("Android rejected the Back action.")
        }
        return verify(before, 2500L, "Pressed Back.")
    }

    private fun waitStep(step: AutoAction): ActionResult {
        val ms = step.arg.toLongOrNull() ?: 1000L
        val capped = if (ms > 30000L) 30000L else ms
        return if (sleepChecked(capped)) {
            ActionResult(ActionState.SUCCEEDED, "Waited.")
        } else {
            cancelledResult()
        }
    }

    private fun readScreen(): ActionResult {
        val obs = ScreenObserver.observe()
        mainHandler.post { ObserverState.latest = obs }
        return if (obs.ok) {
            ActionResult(
                ActionState.SUCCEEDED,
                "Read " + obs.elements.size + " elements in " + obs.packageName +
                    ". See the Observer tab."
            )
        } else {
            fail(obs.message)
        }
    }
}
