package com.stylekeyboard.app.ui.screens.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stylekeyboard.app.app.ServiceLocator
import com.stylekeyboard.app.data.db.entity.PresetEntity
import com.stylekeyboard.app.data.model.JsonCodec
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PresetsViewModel : ViewModel() {
    private val repo = ServiceLocator.presetRepository

    val presets: StateFlow<List<PresetEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun create(name: String, description: String, mapping: Map<String, String>, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.insert(name, mapping, description)
            onCreated(id)
        }
    }

    fun update(preset: PresetEntity, name: String, description: String, mapping: Map<String, String>) {
        viewModelScope.launch { repo.update(preset, mapping, name, description) }
    }

    fun duplicate(preset: PresetEntity) {
        viewModelScope.launch { repo.duplicate(preset) }
    }

    fun delete(preset: PresetEntity) {
        viewModelScope.launch { repo.delete(preset) }
    }

    fun decodeMapping(preset: PresetEntity): Map<String, String> = JsonCodec.decodeMap(preset.mappingJson)
}
