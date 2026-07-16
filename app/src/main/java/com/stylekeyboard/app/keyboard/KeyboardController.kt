package com.stylekeyboard.app.keyboard

import android.view.inputmethod.InputConnection
import com.stylekeyboard.app.app.ServiceLocator
import com.stylekeyboard.app.data.db.entity.PresetEntity
import com.stylekeyboard.app.data.db.entity.ShortcutEntity
import com.stylekeyboard.app.data.model.JsonCodec
import com.stylekeyboard.app.data.repository.ResolvedConfig
import com.stylekeyboard.app.util.TextConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared state holder for the keyboard. The [StyleKeyboardService] owns one
 * instance and passes it down to the Compose [KeyboardScreen].
 *
 * It is responsible for:
 *   - Loading the active preset's mapping and exposing it as an in-memory Map
 *     so per-keystroke conversion is allocation-free.
 *   - Loading the shortcut list once per input session for the suggestion strip.
 *   - Holding the suggestion state (the 3 word slots + emoji-shortcut slots).
 *   - Calling the prediction repository to update bigrams/trigrams on word
 *     boundaries, in the background.
 */
class KeyboardController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _activePreset = MutableStateFlow<PresetEntity?>(null)
    val activePreset: StateFlow<PresetEntity?> = _activePreset.asStateFlow()

    private val _activeMapping = MutableStateFlow<Map<String, String>>(emptyMap())
    val activeMapping: StateFlow<Map<String, String>> = _activeMapping.asStateFlow()

    private val _config = MutableStateFlow(ResolvedConfig())
    val config: StateFlow<ResolvedConfig> = _config.asStateFlow()

    private val _shortcuts = MutableStateFlow<List<ShortcutEntity>>(emptyList())
    val shortcuts: StateFlow<List<ShortcutEntity>> = _shortcuts.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _emojiSuggestions = MutableStateFlow<List<ShortcutEntity>>(emptyList())
    val emojiSuggestions: StateFlow<List<ShortcutEntity>> = _emojiSuggestions.asStateFlow()

    private val _shiftOn = MutableStateFlow(false)
    val shiftOn: StateFlow<Boolean> = _shiftOn.asStateFlow()

    var soundManager: KeySoundManager? = null
        private set

    /**
     * Re-load the active preset + config + shortcuts. Called when the keyboard
     * starts a new input connection or when the user changes the active preset
     * via the toolbar.
     *
     * The whole body is wrapped in try/catch so a transient DB or JSON error
     * never crashes the input path — the keyboard will fall back to defaults
     * and the user can keep typing.
     */
    fun refresh(context: android.content.Context) {
        if (soundManager == null) soundManager = KeySoundManager(context)
        scope.launch {
            try {
                val configRepo = ServiceLocator.appConfigRepository
                val config = configRepo.get()
                if (config == null) return@launch
                val resolved = ResolvedConfig(
                    gifBackgroundUri = config.gifBackgroundUri,
                    glint = JsonCodec.decodeGlint(config.glintConfigJson),
                    keyShape = config.keyShape,
                    sound = JsonCodec.decodeSound(config.soundConfigJson),
                    hapticsEnabled = config.hapticsEnabled,
                    activePresetId = config.activePresetId,
                    emojiShortcutsEnabled = config.emojiShortcutsEnabled,
                    predictiveTextEnabled = config.predictiveTextEnabled
                )
                _config.value = resolved
                soundManager?.applyConfig(resolved.sound)

                val preset = ServiceLocator.presetRepository.getById(resolved.activePresetId)
                _activePreset.value = preset
                _activeMapping.value = preset?.let { JsonCodec.decodeMap(it.mappingJson) } ?: emptyMap()

                if (resolved.emojiShortcutsEnabled) {
                    _shortcuts.value = ServiceLocator.shortcutRepository.getAll()
                } else {
                    _shortcuts.value = emptyList()
                }
            } catch (t: Throwable) {
                android.util.Log.w("KeyboardController", "refresh failed", t)
            }
        }
    }

    /**
     * Convert a single character through the active preset. Called per keypress.
     */
    fun convertChar(ch: Char): String {
        val map = _activeMapping.value
        return TextConverter.convertChar(ch, map)
    }

    /**
     * User tapped a letter key. Convert + commit to the input connection, then
     * update suggestions + emoji shortcuts based on the current word buffer.
     */
    fun handleLetter(ch: Char, ic: InputConnection?) {
        val effective = if (_shiftOn.value) ch.uppercaseChar() else ch
        val out = convertChar(effective)
        ic?.commitText(out, 1)
        if (_shiftOn.value) _shiftOn.value = false

        // Update suggestions based on the current word being typed
        scope.launch {
            updateSuggestionsForCurrentWord(ic)
            updateEmojiShortcutsForCurrentWord(ic)
        }
    }

    fun handleSpace(ic: InputConnection?) {
        ic?.commitText(" ", 1)
        scope.launch { recordWordBoundary(ic) }
    }

    fun handleDelete(ic: InputConnection?) {
        ic?.deleteSurroundingText(1, 0)
        scope.launch { updateSuggestionsForCurrentWord(ic) }
    }

    fun handleEnter(ic: InputConnection?) {
        ic?.commitText("\n", 1)
        scope.launch { recordWordBoundary(ic) }
    }

    fun toggleShift() { _shiftOn.value = !_shiftOn.value }

    fun insertSuggestion(word: String, ic: InputConnection?) {
        // Replace the current partial word with the suggestion
        val currentWord = currentWordBuffer(ic) ?: ""
        if (currentWord.isNotBlank()) {
            ic?.deleteSurroundingText(currentWord.length, 0)
        }
        ic?.commitText(word, 1)
        scope.launch { updateSuggestionsForCurrentWord(ic) }
    }

    fun insertEmoji(shortcut: ShortcutEntity, ic: InputConnection?) {
        val currentWord = currentWordBuffer(ic) ?: ""
        if (currentWord.isNotBlank()) {
            ic?.deleteSurroundingText(currentWord.length, 0)
        }
        ic?.commitText(shortcut.emojis + " ", 1)
        scope.launch { recordWordBoundary(ic) }
    }

    private fun currentWordBuffer(ic: InputConnection?): String? {
        if (ic == null) return null
        val before = ic.getTextBeforeCursor(40, 0) ?: return null
        val m = Regex("[A-Za-z']+$").find(before.toString()) ?: return ""
        return m.value
    }

    private suspend fun updateSuggestionsForCurrentWord(ic: InputConnection?) {
        if (!_config.value.predictiveTextEnabled) {
            _suggestions.value = emptyList()
            return
        }
        val current = currentWordBuffer(ic) ?: ""
        val before = ic?.getTextBeforeCursor(80, 0)?.toString() ?: ""
        val words = Regex("[A-Za-z']+").findAll(before).map { it.value.lowercase() }.toList()
        val partial = current.lowercase()
        val suggestions = ServiceLocator.predictionRepository.suggest(
            partial = partial,
            previousWords = if (partial.isEmpty()) words else words.dropLast(1),
            slots = 3
        )
        _suggestions.value = suggestions
    }

    private suspend fun updateEmojiShortcutsForCurrentWord(ic: InputConnection?) {
        val current = currentWordBuffer(ic)?.lowercase() ?: ""
        if (current.isBlank()) {
            _emojiSuggestions.value = emptyList()
            return
        }
        val matches = _shortcuts.value.filter { sc ->
            when (sc.triggerMode) {
                "whole" -> sc.trigger == current
                else -> sc.trigger.startsWith(current) && current.length >= 2
            }
        }
        _emojiSuggestions.value = matches
    }

    private suspend fun recordWordBoundary(ic: InputConnection?) {
        if (ic == null) return
        val before = ic.getTextBeforeCursor(120, 0)?.toString() ?: return
        val words = Regex("[A-Za-z']+").findAll(before).map { it.value }.toList()
        if (words.isEmpty()) return
        val confirmed = words.last()
        val previous = if (words.size >= 2) words.dropLast(1) else emptyList()
        ServiceLocator.predictionRepository.recordConfirmedWord(confirmed, previous)
        // After a word boundary, refresh suggestions to show "next word" predictions
        updateSuggestionsForCurrentWord(ic)
    }

    fun release() {
        soundManager?.release()
    }
}
