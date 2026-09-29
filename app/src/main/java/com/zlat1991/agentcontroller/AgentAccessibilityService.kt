package com.zlat1991.agentcontroller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AgentAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Экран будет считываться по запросу агента.
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun getScreenText(): String {
        val root = rootInActiveWindow ?: return ""

        val result = StringBuilder()
        collectText(root, result)

        return result.toString().take(12000)
    }

    private fun collectText(
        node: AccessibilityNodeInfo,
        result: StringBuilder
    ) {
        node.text?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let {
                result.append(it)
                result.append("\n")
            }

        node.contentDescription?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let {
                result.append(it)
                result.append("\n")
            }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                collectText(it, result)
            }
        }
    }

    fun tap(x: Float, y: Float): Boolean {
        val path = Path()
        path.moveTo(x, y)

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0,
                    80
                )
            )
            .build()

        return dispatchGesture(
            gesture,
            null,
            null
        )
    }

    fun swipe(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long
    ): Boolean {

        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0,
                    duration.coerceIn(100, 3000)
                )
            )
            .build()

        return dispatchGesture(
            gesture,
            null,
            null
        )
    }

    fun tapText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false

        val node = findText(root, text)
            ?: return false

        return node.performAction(
            AccessibilityNodeInfo.ACTION_CLICK
        )
    }

    private fun findText(
        node: AccessibilityNodeInfo,
        text: String
    ): AccessibilityNodeInfo? {

        val nodeText = node.text?.toString()
        val description = node.contentDescription?.toString()

        if (
            nodeText?.contains(text, ignoreCase = true) == true ||
            description?.contains(text, ignoreCase = true) == true
        ) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val result = findText(child, text)

            if (result != null) {
                return result
            }
        }

        return null
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow
            ?: return false

        val focused = root.findFocus(
            AccessibilityNodeInfo.FOCUS_INPUT
        ) ?: return false

        val arguments = Bundle()

        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )

        return focused.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            arguments
        )
    }

    fun back(): Boolean {
        return performGlobalAction(
            GLOBAL_ACTION_BACK
        )
    }

    fun home(): Boolean {
        return performGlobalAction(
            GLOBAL_ACTION_HOME
        )
    }
}
