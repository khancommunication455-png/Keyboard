package com.stylekeyboard.app.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
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
 * The system-wide custom keyboard IME. Renders a Compose-based [KeyboardScreen]
 * inside an Android View hierarchy attached to the input method window.
 *
 * The IME lifecycle MUST be wired up correctly for ComposeView to attach.
 * Sequence Android calls:
 *   onCreate()                          → dispatch ON_CREATE
 *   onCreateInputView()                 → build ComposeView, set view-tree owners
 *   onWindowShown()                     → dispatch ON_START, ON_RESUME
 *   onWindowHidden()                    → dispatch ON_PAUSE, ON_STOP
 *   onDestroy()                         → dispatch ON_DESTROY
 *
 * The view tree owners MUST be set BEFORE [ComposeView.setContent] is called,
 * otherwise Compose throws "ViewTreeLifecycleOwner not found from View".
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
        try {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            ServiceLocator.init(this)
        } catch (t: Throwable) {
            Log.e("StyleKeyboardService", "onCreate failed", t)
        }
    }

    override fun onCreateInputView(): View {
        return try {
            // Build a ComposeView. We MUST set the view tree owners BEFORE
            // setContent so the first composition can find them.
            val view = androidx.compose.ui.platform.ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@StyleKeyboardService)
                setViewTreeSavedStateRegistryOwner(this@StyleKeyboardService)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                // Important: layout params so the IME measures the view
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
                        onOpenEmojiPanel = { /* future: open emoji panel */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            // Refresh config + presets in the background so the first paint isn't
            // blocked on a DB read.
            runCatching { controller.refresh(this) }
            viewCreated = true
            view
        } catch (t: Throwable) {
            Log.e("StyleKeyboardService", "onCreateInputView failed", t)
            // Fallback: empty view so we don't crash the system
            View(this)
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        try {
            if (viewCreated) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
        } catch (t: Throwable) {
            Log.w("StyleKeyboardService", "onWindowShown failed", t)
        }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        try {
            if (viewCreated) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        } catch (t: Throwable) {
            Log.w("StyleKeyboardService", "onWindowHidden failed", t)
        }
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        runCatching { controller.refresh(this) }
    }

    override fun onDestroy() {
        runCatching { controller.release() }
        try {
            if (viewCreated) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
        } catch (t: Throwable) {
            Log.w("StyleKeyboardService", "onDestroy failed", t)
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
                Key.ActionType.Symbol -> { /* handled in KeyboardScreen for layout switch */ }
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
            Log.w("StyleKeyboardService", "switchToNextInputMethod failed", t)
        }
    }

    private fun launchSettings() {
        try {
            val intent = android.content.Intent(this, com.stylekeyboard.app.ui.MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (t: Throwable) {
            Log.w("StyleKeyboardService", "Failed to launch settings", t)
        }
    }

    private fun showPresetSwitcher() {
        // For brevity, just open the host app's Presets screen.
        launchSettings()
    }
}
