package com.maciejhetman.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciejhetman.notes.data.AppSettings
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.data.NoteFontSize
import com.maciejhetman.notes.data.SettingsRepository
import com.maciejhetman.notes.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch { repository.setAmoledBlack(enabled) }
    }

    fun setLineNumberMode(mode: LineNumberMode) {
        viewModelScope.launch { repository.setLineNumberMode(mode) }
    }

    fun setFontSize(size: NoteFontSize) {
        viewModelScope.launch { repository.setFontSize(size) }
    }
}
