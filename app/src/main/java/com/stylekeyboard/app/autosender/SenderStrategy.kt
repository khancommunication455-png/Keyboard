package com.stylekeyboard.app.autosender

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager
import com.stylekeyboard.app.data.model.AutoSenderScript

/**
 * Picks the right send strategy for [script]:
 *   - If [script.useAccessibility] is false: fire an [Intent.ACTION_SEND]
 *     chooser. Works for any app that registers a share-intent (most messengers).
 *   - If true: dispatch to [AutoSenderAccessibilityService.send] against the
 *     target package's foreground EditText.
 *
 * Returns true on apparent success. Note: ACTION_SEND returns immediately
 * (the chooser appears) — we record "sent" optimistically; the user closes the
 * chooser themselves. The Accessibility path is synchronous and more reliable.
 */
object SenderStrategy {

    fun send(context: Context, script: AutoSenderScript, message: String): Boolean {
        return if (script.useAccessibility) {
            sendViaAccessibility(context, script, message)
        } else {
            sendViaShareIntent(context, script, message)
        }
    }

    private fun sendViaShareIntent(context: Context, script: AutoSenderScript, message: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            if (script.targetPackage.isNotBlank()) {
                setPackage(script.targetPackage)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, "Send via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (t: Throwable) {
            false
        }
    }

    private fun sendViaAccessibility(context: Context, script: AutoSenderScript, message: String): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (!am.isEnabled) return false
        // Find our running AutoSenderAccessibilityService instance via the
        // static singleton wired up in onServiceConnected. We use a simple
        // holder because AccessibilityService instances can't be looked up
        // by the system directly.
        val service = AutoSenderAccessibilityServiceHolder.instance ?: return false
        val targetPkg = script.targetPackage.ifBlank { return false }
        // Bridge to the suspend send via a blocking wait
        val result = kotlinx.coroutines.runBlocking {
            AutoSenderAccessibilityService.send(service, targetPkg, message)
        }
        return result
    }
}

/** Holds a reference to the live accessibility service instance. */
object AutoSenderAccessibilityServiceHolder {
    @Volatile var instance: AutoSenderAccessibilityService? = null
}
