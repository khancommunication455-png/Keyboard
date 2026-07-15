package com.stylekeyboard.app.ui.screens.emojilab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stylekeyboard.app.app.ServiceLocator
import com.stylekeyboard.app.data.db.entity.ShortcutEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmojiLabViewModel : ViewModel() {
    private val repo = ServiceLocator.shortcutRepository

    val shortcuts: StateFlow<List<ShortcutEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun add(trigger: String, emojis: String, mode: String) {
        viewModelScope.launch { repo.insert(trigger, emojis, mode) }
    }

    fun update(entity: ShortcutEntity, trigger: String, emojis: String, mode: String) {
        viewModelScope.launch { repo.update(entity, trigger, emojis, mode) }
    }

    fun delete(entity: ShortcutEntity) {
        viewModelScope.launch { repo.delete(entity) }
    }

    fun export(): String = kotlinx.coroutines.runBlocking { repo.exportJson() }

    fun import(json: String) {
        viewModelScope.launch { repo.importJson(json) }
    }
}
