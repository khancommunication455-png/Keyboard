package com.stylekeyboard.app.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
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
 * Lifecycle wiring notes (this is the part most Compose-IME tutorials get wrong):
 *   - An `InputMethodService` is a Service, not an Activity. Compose's
 *     [androidx.compose.ui.platform.ComposeView] needs a `LifecycleOwner`
 *     that goes through CREATED → STARTED → RESUMED → PAUSED → STOPPED → DESTROYED,
 *     otherwise `remember`/`collectAsState` won't recompose.
 *   - We dispatch ON_CREATE in [onCreate], ON_START+ON_RESUME in [onWindowShown],
 *     ON_PAUSE+ON_STOP in [onWindowHidden], and ON_DESTROY in [onDestroy].
 *   - We also implement [SavedStateRegistryOwner] because ComposeView requires
 *     one in the view tree.
 */
class StyleKeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val controller = KeyboardController()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        ServiceLocator.init(this)
    }

    override fun onCreateInputView(): View {
        // Build a ComposeView attached to the IME's lifecycle. The view tree
        // owners MUST be set BEFORE setContent, otherwise the first composition
        // will throw "ViewTreeLifecycleOwner not found".
        val view = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@StyleKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@StyleKeyboardService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Refresh config + presets in the background so the first paint isn't
        // blocked on a DB read.
        controller.refresh(this)
        return view
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // Refresh on each new input field so a config change in the host app
        // (e.g. the user picked a new active preset) is picked up.
        runCatching { controller.refresh(this) }
    }

    override fun onDestroy() {
        runCatching { controller.release() }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    private fun handleKey(key: Key) {
        val ic = currentInputConnection
        // Play sound + haptics first so the click aligns with the tap. Wrap in
        // runCatching so a bad SoundPool state can never crash the input path.
        val config = controller.config.value
        runCatching { controller.soundManager?.playClick(config.sound, config.hapticsEnabled) }

        when (key) {
            is Key.Letter -> controller.handleLetter(key.label.firstOrNull() ?: ' ', ic)
            is Key.Action -> when (key.type) {
                Key.ActionType.Space -> controller.handleSpace(ic)
                Key.ActionType.Delete -> controller.handleDelete(ic)
                Key.ActionType.Enter -> controller.handleEnter(ic)
                Key.ActionType.Shift -> controller.toggleShift()
                Key.ActionType.Symbol -> { /* extension point: switch to symbol layout */ }
                Key.ActionType.SwitchKeyboard -> switchToNextKeyboardOrSystem()
                Key.ActionType.SwitchPreset -> showPresetSwitcher()
                Key.ActionType.Settings -> launchSettings()
                Key.ActionType.Suggestion -> { /* handled via onSuggestionTap */ }
            }
        }
    }

    private fun switchToNextKeyboardOrSystem() {
        // Use the IME's own API rather than InputMethodManager.showInputMethodPicker,
        // which throws when called from a non-Activity (Service) context.
        try {
            val switched = switchToNextInputMethod(false)
            if (!switched) {
                // No next input method — fall back to the system IME picker via
                // the Settings intent, launched from our own activity task.
                Toast.makeText(this, "No other keyboard to switch to.", Toast.LENGTH_SHORT).show()
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
        // For brevity, just open the host app's Presets screen. A production
        // version would show a popup dialog with a horizontal list of presets.
        launchSettings()
    }
}
