package com.stylekeyboard.app.keyboard

import android.inputmethodservice.InputMethodService
import android.os.IBinder
import android.view.View
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
 * Per-keystroke pipeline:
 *   1. User taps a key on [KeyboardScreen]
 *   2. We dispatch to [KeyboardController] which:
 *      a. Applies the active preset's char mapping (allocation-free)
 *      b. Updates emoji-shortcut suggestions (background coroutine)
 *      c. Updates predictive-text suggestions (background coroutine)
 *      d. Plays the key sound via [KeySoundManager]
 *   3. The controller commits the converted text via the [InputConnection]
 *
 * On word boundaries (space/enter/delete), the controller records the word in
 * the unigram/bigram/trigram tables on a background coroutine so the input
 * thread never blocks.
 */
class StyleKeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val controller = KeyboardController()
    private var composeView: androidx.compose.ui.platform.ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        ServiceLocator.init(this)
    }

    override fun onCreateInputView(): View {
        // Build a ComposeView attached to the IME's lifecycle
        val view = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@StyleKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@StyleKeyboardService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDisposed)
        }
        composeView = view

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
        controller.refresh(this)
        return view
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        controller.refresh(this)
    }

    override fun onDestroy() {
        controller.release()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    private fun handleKey(key: Key) {
        val ic = currentInputConnection
        // Play sound + haptics first so the click aligns with the tap
        val config = controller.config.value
        controller.soundManager?.playClick(config.sound, config.hapticsEnabled)

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
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
        imm.showInputMethodPicker()
    }

    private fun launchSettings() {
        val intent = android.content.Intent(this, com.stylekeyboard.app.ui.MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun showPresetSwitcher() {
        // For brevity, just open the host app's Presets screen. A production
        // version would show a popup dialog with a horizontal list of presets.
        launchSettings()
    }

    override fun onCreateInputMethodInterface(): AbstractInputMethodImpl {
        return super.onCreateInputMethodInterface()
    }
}
