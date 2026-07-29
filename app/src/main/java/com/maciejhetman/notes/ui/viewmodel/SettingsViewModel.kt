package com.maciejhetman.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciejhetman.notes.data.AppSettings
import com.maciejhetman.notes.data.EditorLineSpacing
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.data.NoteFontFamily
import com.maciejhetman.notes.data.SettingsRepository
import com.maciejhetman.notes.data.SyntaxTheme
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

    fun setFontSizeScale(scale: Float) {
        viewModelScope.launch { repository.setFontSizeScale(scale) }
    }

    fun setFontFamily(family: NoteFontFamily) {
        viewModelScope.launch { repository.setFontFamily(family) }
    }

    fun setSyntaxTheme(theme: SyntaxTheme) {
        viewModelScope.launch { repository.setSyntaxTheme(theme) }
    }

    fun setLineSpacing(spacing: EditorLineSpacing) {
        viewModelScope.launch { repository.setLineSpacing(spacing) }
    }
}

