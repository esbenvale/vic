package com.vale.vic.automation.observer

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.vale.vic.automation.service.VicAccessibilityService

/**
 * Reads the accessible interface of the active window.
 * Never reads password fields or the contents of text fields.
 */
object ScreenObserver {

    private const val MAX_NODES = 300
    private const val MAX_DEPTH = 30

    private class Walker {
        var count = 0
        var truncated = false
        val out = ArrayList<UiElement>()
    }

    fun observe(): ScreenObservation {
        val now = System.currentTimeMillis()
        val service = VicAccessibilityService.instance
            ?: return ScreenObservation(
                false,
                "VIC's accessibility service is not connected.",
                "",
                emptyList(),
                false,
                now
            )
        val root = service.rootInActiveWindow
            ?: return ScreenObservation(
                false,
                "No readable window right now. The screen may be protected or still loading.",
                "",
                emptyList(),
                false,
                now
            )
        val walker = Walker()
        val pkg = root.packageName?.toString() ?: ""
        walk(root, 0, walker)
        recycleQuietly(root)
        return ScreenObservation(true, "OK", pkg, walker.out, walker.truncated, now)
    }

    private fun walk(node: AccessibilityNodeInfo?, depth: Int, w: Walker) {
        if (node == null) return
        if (w.count >= MAX_NODES) {
            w.truncated = true
            return
        }
        w.count++
        if (node.isVisibleToUser) {
            val element = toElement(node, depth)
            if (element != null) w.out.add(element)
        }
        if (depth >= MAX_DEPTH) {
            w.truncated = true
            return
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            walk(child, depth + 1, w)
            recycleQuietly(child)
        }
    }

    private fun toElement(n: AccessibilityNodeInfo, depth: Int): UiElement? {
        val editable = n.isEditable
        val label: String = when {
            n.isPassword -> "[password field - hidden]"
            editable -> "[text field - contents not read]"
            else -> (n.text?.toString() ?: n.contentDescription?.toString() ?: "")
        }.trim().take(80)

        val interactive = n.isClickable || editable || n.isScrollable || n.isCheckable
        if (label.isEmpty() && !interactive) return null

        val rect = Rect()
        n.getBoundsInScreen(rect)
        val viewId = (n.viewIdResourceName ?: "").substringAfter(":id/")

        return UiElement(
            label = label,
            kind = kindOf(n),
            viewId = viewId,
            clickable = n.isClickable,
            editable = editable,
            scrollable = n.isScrollable,
            checkable = n.isCheckable,
            checked = n.isChecked,
            enabled = n.isEnabled,
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            depth = depth
        )
    }

    private fun kindOf(n: AccessibilityNodeInfo): String {
        val cls = n.className?.toString() ?: ""
        return when {
            n.isEditable -> "Field"
            cls.contains("Button") -> "Button"
            cls.contains("Switch") || cls.contains("Toggle") -> "Switch"
            cls.contains("CheckBox") -> "Checkbox"
            cls.contains("RadioButton") -> "Radio"
            cls.contains("Image") -> "Image"
            cls.contains("RecyclerView") || cls.contains("ListView") ||
                cls.contains("ScrollView") -> "List"
            cls.contains("TextView") -> "Text"
            n.isClickable -> "Clickable"
            else -> cls.substringAfterLast('.').ifEmpty { "View" }
        }
    }

    @Suppress("DEPRECATION")
    private fun recycleQuietly(node: AccessibilityNodeInfo?) {
        try {
            node?.recycle()
        } catch (e: Exception) {
            // Recycling is a no-op on newer Android versions.
        }
    }
}
