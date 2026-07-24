package com.maciejhetman.notes.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LineNumberMode { OFF, ALL_LINES, CODE_BLOCKS_ONLY }

enum class NoteFontSize(val scale: Float, val label: String) {
    SMALL(0.85f, "Small"),
    DEFAULT(1f, "Default"),
    LARGE(1.15f, "Large"),
    EXTRA_LARGE(1.3f, "Extra large")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val amoledBlack: Boolean = false,
    val lineNumberMode: LineNumberMode = LineNumberMode.OFF,
    val fontSize: NoteFontSize = NoteFontSize.DEFAULT
)

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val LINE_NUMBER_MODE = stringPreferencesKey("line_number_mode")
        val FONT_SIZE = stringPreferencesKey("font_size")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.toEnumOrNull<ThemeMode>() ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            amoledBlack = prefs[Keys.AMOLED_BLACK] ?: false,
            lineNumberMode = prefs[Keys.LINE_NUMBER_MODE]?.toEnumOrNull<LineNumberMode>() ?: LineNumberMode.OFF,
            fontSize = prefs[Keys.FONT_SIZE]?.toEnumOrNull<NoteFontSize>() ?: NoteFontSize.DEFAULT
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AMOLED_BLACK] = enabled }
    }

    suspend fun setLineNumberMode(mode: LineNumberMode) {
        context.settingsDataStore.edit { it[Keys.LINE_NUMBER_MODE] = mode.name }
    }

    suspend fun setFontSize(size: NoteFontSize) {
        context.settingsDataStore.edit { it[Keys.FONT_SIZE] = size.name }
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    runCatching { enumValueOf<T>(this) }.getOrNull()
