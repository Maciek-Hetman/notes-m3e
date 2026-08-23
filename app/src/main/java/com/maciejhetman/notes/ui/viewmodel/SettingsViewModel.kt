package com.maciejhetman.notes.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciejhetman.notes.data.AppSettings
import com.maciejhetman.notes.data.EditorLineSpacing
import com.maciejhetman.notes.data.IndentGuideColor
import com.maciejhetman.notes.data.IndentGuideStyle
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.data.NoteFontFamily
import com.maciejhetman.notes.data.SettingsRepository
import com.maciejhetman.notes.data.SyntaxTheme
import com.maciejhetman.notes.data.ThemeMode
import kotlin.coroutines.cancellation.CancellationException
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
        viewModelScope.launch {
            try {
                repository.setThemeMode(mode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set theme mode", e)
            }
        }
    }

    fun setThemeColor(color: com.maciejhetman.notes.data.AppThemeColor) {
        viewModelScope.launch {
            try {
                repository.setThemeColor(color)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set theme color", e)
            }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setDynamicColor(enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set dynamic color", e)
            }
        }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setAmoledBlack(enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set amoled black", e)
            }
        }
    }

    fun setLineNumberMode(mode: LineNumberMode) {
        viewModelScope.launch {
            try {
                repository.setLineNumberMode(mode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set line number mode", e)
            }
        }
    }

    fun setFontSizeScale(scale: Float) {
        viewModelScope.launch {
            try {
                repository.setFontSizeScale(scale)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set font size scale", e)
            }
        }
    }

    fun setFontFamily(family: NoteFontFamily) {
        viewModelScope.launch {
            try {
                repository.setFontFamily(family)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set font family", e)
            }
        }
    }

    fun setSyntaxTheme(theme: SyntaxTheme) {
        viewModelScope.launch {
            try {
                repository.setSyntaxTheme(theme)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set syntax theme", e)
            }
        }
    }

    fun setLineSpacing(spacing: EditorLineSpacing) {
        viewModelScope.launch {
            try {
                repository.setLineSpacing(spacing)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set line spacing", e)
            }
        }
    }

    fun setEnabledLanguages(languages: Set<String>) {
        viewModelScope.launch {
            try {
                repository.setEnabledLanguages(languages)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set enabled languages", e)
            }
        }
    }

    fun setTextIndentDepthSp(depthSp: Float) {
        viewModelScope.launch {
            try {
                repository.setTextIndentDepthSp(depthSp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set text indent depth", e)
            }
        }
    }

    fun setTextIndentColor(color: IndentGuideColor) {
        viewModelScope.launch {
            try {
                repository.setTextIndentColor(color)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set text indent color", e)
            }
        }
    }

    fun setTextIndentStyle(style: IndentGuideStyle) {
        viewModelScope.launch {
            try {
                repository.setTextIndentStyle(style)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set text indent style", e)
            }
        }
    }

    fun setCodeIndentDepthSp(depthSp: Float) {
        viewModelScope.launch {
            try {
                repository.setCodeIndentDepthSp(depthSp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set code indent depth", e)
            }
        }
    }

    fun setCodeIndentColor(color: IndentGuideColor) {
        viewModelScope.launch {
            try {
                repository.setCodeIndentColor(color)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set code indent color", e)
            }
        }
    }

    fun setCodeIndentStyle(style: IndentGuideStyle) {
        viewModelScope.launch {
            try {
                repository.setCodeIndentStyle(style)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set code indent style", e)
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}

