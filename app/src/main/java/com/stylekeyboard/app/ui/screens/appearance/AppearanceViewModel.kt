package com.stylekeyboard.app.ui.screens.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stylekeyboard.app.app.ServiceLocator
import com.stylekeyboard.app.data.model.GlintConfig
import com.stylekeyboard.app.data.model.SoundConfig
import com.stylekeyboard.app.data.repository.ResolvedConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearanceViewModel : ViewModel() {
    private val repo = ServiceLocator.appConfigRepository

    val config: StateFlow<ResolvedConfig?> = repo.observeResolved()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setGif(uri: String) = viewModelScope.launch { repo.setGifBackground(uri) }
    fun setGlint(cfg: GlintConfig) = viewModelScope.launch { repo.setGlint(cfg) }
    fun setKeyShape(shape: String) = viewModelScope.launch { repo.setKeyShape(shape) }
    fun setSound(cfg: SoundConfig) = viewModelScope.launch { repo.setSound(cfg) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { repo.setHaptics(enabled) }
}
