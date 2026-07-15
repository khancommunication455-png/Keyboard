package com.stylekeyboard.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stylekeyboard.app.app.ServiceLocator
import com.stylekeyboard.app.data.db.entity.PresetEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val presetRepo = ServiceLocator.presetRepository
    private val configRepo = ServiceLocator.appConfigRepository

    val presets: StateFlow<List<PresetEntity>> = presetRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activePresetId: StateFlow<Long> = configRepo.observe()
        .map { it?.activePresetId ?: 1L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1L)

    fun setActivePreset(id: Long) {
        viewModelScope.launch { configRepo.setActivePreset(id) }
    }
}
