package com.stylekeyboard.app.ui.screens.smarttyping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stylekeyboard.app.app.ServiceLocator
import com.stylekeyboard.app.data.db.entity.BigramEntity
import com.stylekeyboard.app.data.db.entity.UserDictionaryEntity
import com.stylekeyboard.app.data.db.entity.WordFrequencyEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmartTypingViewModel : ViewModel() {
    private val repo = ServiceLocator.predictionRepository

    val topWords: StateFlow<List<WordFrequencyEntity>> =
        repo.observeTopWords(100).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val topBigrams: StateFlow<List<BigramEntity>> =
        repo.observeTopBigrams(50).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val userDictionary: StateFlow<List<UserDictionaryEntity>> =
        repo.observeUserDictionary().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addWord(word: String) {
        viewModelScope.launch { repo.addUserWord(word) }
    }

    fun removeWord(word: String) {
        viewModelScope.launch { repo.removeUserWord(word) }
    }

    fun clearHistory() {
        viewModelScope.launch { repo.clearLearnedHistory() }
    }

    fun runDecay() {
        viewModelScope.launch { repo.runDecay() }
    }
}
