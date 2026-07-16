package com.stylekeyboard.app.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.stylekeyboard.app.app.ServiceLocator
import com.stylekeyboard.app.ui.theme.StyleKeyboardTheme

/**
 * The system-wide custom keyboard IME.
 *
 * Defensive coding: every override is wrapped in try/catch so a failure in any
 * step never crashes the IME process (which would make the system fall back to
 * the previous keyboard and confuse the user).
 *
 * If the Compose view fails to create, we fall back to a simple native
 * LinearLayout with a few buttons so the user at least sees *something* and
 * can switch back to their previous keyboard via the globe icon.
 */
class StyleKeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val controller = KeyboardController()
    @Volatile private var viewCreated = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        try {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            ServiceLocator.init(this)
        } catch (t: Throwable) {
            Log.e(TAG, "onCreate failed", t)
        }
    }

    override fun onCreateInputView(): View {
        Log.i(TAG, "onCreateInputView")
        return try {
            // Ensure ServiceLocator is initialized even if onCreate threw
            runCatching { ServiceLocator.init(this) }

            // CRITICAL: Compose looks for ViewTreeLifecycleOwner/SavedStateRegistryOwner
            // on the *window's root decor view* (the "parentPanel" InputMethodService
            // creates internally), not on the ComposeView we return below. Without this,
            // ComposeView.onAttachedToWindow throws IllegalStateException
            // ("ViewTreeLifecycleOwner not found from ...parentPanel") the first time the
            // keyboard is shown — and since that happens during onAttachedToWindow, it's
            // NOT caught by this try/catch, crashing the process outright.
            window?.window?.decorView?.let { decor ->
                decor.setViewTreeLifecycleOwner(this@StyleKeyboardService)
                decor.setViewTreeSavedStateRegistryOwner(this@StyleKeyboardService)
            }

            val view = androidx.compose.ui.platform.ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@StyleKeyboardService)
                setViewTreeSavedStateRegistryOwner(this@StyleKeyboardService)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            view.setContent {
                StyleKeyboardTheme {
                    KeyboardScreen(
                        controller = controller,
                        onKey = { key -> handleKey(key) },
                        onSuggestionTap = { word -> controller.insertSuggestion(word, currentInputConnection) },
                        onEmojiShortcutTap = { sc -> controller.insertEmoji(sc, currentInputConnection) },
                        onGlobe = { switchToNextKeyboardOrSystem() },
                        onSettings = { launchSettings() },
                        onSwitchPreset = { showPresetSwitcher() },
                        onOpenEmojiPanel = { /* future */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            runCatching { controller.refresh(this) }
            viewCreated = true
            Log.i(TAG, "onCreateInputView: Compose view created successfully")
            view
        } catch (t: Throwable) {
            Log.e(TAG, "onCreateInputView FAILED — falling back to native view", t)
            createFallbackView()
        }
    }

    /**
     * Minimal native fallback so the keyboard always shows SOMETHING. If the
     * Compose path fails, the user still sees a row of letters and can type
     * and switch back to their old keyboard.
     */
    private fun createFallbackView(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 8, 8, 8)
            setBackgroundColor(0xFF0A0A0A.toInt())
        }

        val label = TextView(this).apply {
            text = "Style Keyboard (fallback mode — restart app to fix)"
            setTextColor(0xFFEDEDED.toInt())
            textSize = 12f
            setPadding(16, 8, 16, 8)
        }
        container.addView(label)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        "qwertyuiop".forEach { ch ->
            row.addView(makeFallbackKey(ch.toString()))
        }
        container.addView(row)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        "asdfghjkl".forEach { ch ->
            row2.addView(makeFallbackKey(ch.toString()))
        }
        container.addView(row2)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val space = Button(this).apply {
            text = "space"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener { currentInputConnection?.commitText(" ", 1) }
        }
        val enter = Button(this).apply {
            text = "⏎"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentInputConnection?.commitText("\n", 1) }
        }
        val del = Button(this).apply {
            text = "⌫"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { currentInputConnection?.deleteSurroundingText(1, 0) }
        }
        val globe = Button(this).apply {
            text = "🌐"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { switchToNextKeyboardOrSystem() }
        }
        controls.addView(globe)
        controls.addView(space)
        controls.addView(del)
        controls.addView(enter)
        container.addView(controls)

        viewCreated = true
        return container
    }

    private fun makeFallbackKey(label: String): Button {
        return Button(this).apply {
            text = label
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                currentInputConnection?.commitText(label, 1)
            }
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        Log.i(TAG, "onWindowShown")
        try {
            // Safety net: reapply in case the decor view wasn't ready yet when
            // onCreateInputView first ran (happens on some OEM ROMs).
            window?.window?.decorView?.let { decor ->
                decor.setViewTreeLifecycleOwner(this@StyleKeyboardService)
                decor.setViewTreeSavedStateRegistryOwner(this@StyleKeyboardService)
            }
            if (viewCreated) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "onWindowShown failed", t)
        }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        Log.i(TAG, "onWindowHidden")
        try {
            if (viewCreated) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "onWindowHidden failed", t)
        }
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        runCatching { controller.refresh(this) }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        runCatching { controller.release() }
        try {
            if (viewCreated) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "onDestroy failed", t)
        }
        super.onDestroy()
    }

    private fun handleKey(key: Key) {
        val ic = currentInputConnection
        val config = controller.config.value
        runCatching { controller.soundManager?.playClick(config.sound, config.hapticsEnabled) }

        when (key) {
            is Key.Letter -> controller.handleLetter(key.label.firstOrNull() ?: ' ', ic)
            is Key.Action -> when (key.type) {
                Key.ActionType.Space -> controller.handleSpace(ic)
                Key.ActionType.Delete -> controller.handleDelete(ic)
                Key.ActionType.Enter -> controller.handleEnter(ic)
                Key.ActionType.Shift -> controller.toggleShift()
                Key.ActionType.Symbol -> { /* handled in KeyboardScreen */ }
                Key.ActionType.SwitchKeyboard -> switchToNextKeyboardOrSystem()
                Key.ActionType.SwitchPreset -> showPresetSwitcher()
                Key.ActionType.Settings -> launchSettings()
                Key.ActionType.Suggestion -> { /* handled via onSuggestionTap */ }
                Key.ActionType.Emoji -> { /* future */ }
                Key.ActionType.Mic -> { /* future */ }
                Key.ActionType.Search -> { /* future */ }
                Key.ActionType.Clipboard -> { /* future */ }
            }
        }
    }

    private fun switchToNextKeyboardOrSystem() {
        try {
            val switched = switchToNextInputMethod(false)
            if (!switched) {
                Toast.makeText(this, "No other keyboard installed.", Toast.LENGTH_SHORT).show()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "switchToNextInputMethod failed", t)
        }
    }

    private fun launchSettings() {
        try {
            val intent = android.content.Intent(this, com.stylekeyboard.app.ui.MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to launch settings", t)
        }
    }

    private fun showPresetSwitcher() {
        launchSettings()
    }

    companion object {
        private const val TAG = "StyleKeyboardService"
    }
}
