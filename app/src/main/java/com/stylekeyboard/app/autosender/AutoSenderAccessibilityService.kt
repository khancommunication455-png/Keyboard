package com.stylekeyboard.app.autosender

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Accessibility Service used by the Auto Sender only when the chosen target
 * app does NOT support share-intents and the user has explicitly opted into
 * the Accessibility strategy in the Auto Sender screen.
 *
 * Strategy:
 *   1. Find a text-edit node inside the target package's foreground window.
 *   2. Set its text to the message via [AccessibilityNodeInfo.ACTION_SET_TEXT].
 *   3. Find a node whose id/cd matches a "send" affordance and click it.
 *
 * If the target package's UI doesn't expose a discoverable EditText or send
 * button (some apps hide them), the send returns false and the Auto Sender
 * logs a "failed" row.
 */
class AutoSenderAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AutoSenderAccessibilityServiceHolder.instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: we drive everything synchronously from [SenderStrategy].
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (AutoSenderAccessibilityServiceHolder.instance === this) {
            AutoSenderAccessibilityServiceHolder.instance = null
        }
    }

    companion object {
        /**
         * Attempt to send [message] into an EditText of [targetPackage] and tap
         * the send button. Returns true if both steps succeeded.
         */
        suspend fun send(
            service: AccessibilityService,
            targetPackage: String,
            message: String
        ): Boolean {
            val root = findTargetRoot(service, targetPackage) ?: return false
            val edit = findEditableNode(root)
            val sendButton = findSendButton(root)

            val okEditText = edit?.let {
                val args = android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                }
                it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            } ?: false

            // Give the target UI a moment to update before tapping send
            delay(150)

            val okSend = sendButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
            return okEditText && okSend
        }

        private suspend fun findTargetRoot(
            service: AccessibilityService,
            targetPackage: String
        ): AccessibilityNodeInfo? {
            return withTimeoutOrNull(3000L) {
                while (true) {
                    val root = service.rootInActiveWindow
                    if (root != null && root.packageName?.toString() == targetPackage) {
                        return@withTimeoutOrNull root
                    }
                    delay(80)
                }
                null
            }
        }

        private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.className?.toString() == "android.widget.EditText") return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findEditableNode(child)?.let { return it }
            }
            return null
        }

        private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            // Heuristic: clickable node whose content-desc or text mentions send
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""
            val isSend = desc.contains("send") || text.contains("send") || desc.contains("senden") || text == ">"
            if (isSend && node.isClickable) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findSendButton(child)?.let { return it }
            }
            return null
        }
    }
}
