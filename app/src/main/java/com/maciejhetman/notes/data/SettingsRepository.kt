package com.maciejhetman.notes.data

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LineNumberMode { OFF, ALL_LINES, CODE_BLOCKS_ONLY }

enum class NoteFontFamily(val label: String) {
    SYSTEM("System default"),
    SANS_SERIF("Google Sans / Sans Serif"),
    SERIF("Serif"),
    MONOSPACE("Monospace"),
    CURSIVE("Cursive / Handwriting");

    fun toComposeFontFamily(): FontFamily = when (this) {
        SYSTEM -> FontFamily.Default
        SANS_SERIF -> FontFamily.SansSerif
        SERIF -> FontFamily.Serif
        MONOSPACE -> FontFamily.Monospace
        CURSIVE -> FontFamily.Cursive
    }
}

enum class SyntaxTheme(val label: String) {
    MATERIAL("Material Accent"),
    MONOKAI("Monokai"),
    DRACULA("Dracula"),
    SOLARIZED("Solarized"),
    GITHUB("GitHub"),
    NORD("Nord")
}

enum class EditorLineSpacing(val multiplier: Float, val label: String) {
    COMPACT(1.1f, "Compact"),
    NORMAL(1.3f, "Normal"),
    RELAXED(1.5f, "Relaxed")
}

enum class AppThemeColor(val label: String) {
    DEFAULT("Default"),
    BLUE("Blue"),
    GREEN("Green"),
    PURPLE("Purple"),
    RED("Red")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: AppThemeColor = AppThemeColor.DEFAULT,
    val dynamicColor: Boolean = true,
    val amoledBlack: Boolean = false,
    val lineNumberMode: LineNumberMode = LineNumberMode.OFF,
    val fontSizeScale: Float = 1.0f,
    val fontFamily: NoteFontFamily = NoteFontFamily.SYSTEM,
    val syntaxTheme: SyntaxTheme = SyntaxTheme.MATERIAL,
    val lineSpacing: EditorLineSpacing = EditorLineSpacing.NORMAL,
    val enabledLanguages: Set<String> = emptySet()
)

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val LINE_NUMBER_MODE = stringPreferencesKey("line_number_mode")
        val FONT_SIZE_SCALE = floatPreferencesKey("font_size_scale")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val SYNTAX_THEME = stringPreferencesKey("syntax_theme")
        val LINE_SPACING = stringPreferencesKey("line_spacing")
        val ENABLED_LANGUAGES = stringSetPreferencesKey("enabled_languages")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.toEnumOrNull<ThemeMode>() ?: ThemeMode.SYSTEM,
            themeColor = prefs[Keys.THEME_COLOR]?.toEnumOrNull<AppThemeColor>() ?: AppThemeColor.DEFAULT,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            amoledBlack = prefs[Keys.AMOLED_BLACK] ?: false,
            lineNumberMode = prefs[Keys.LINE_NUMBER_MODE]?.toEnumOrNull<LineNumberMode>() ?: LineNumberMode.OFF,
            fontSizeScale = prefs[Keys.FONT_SIZE_SCALE] ?: 1.0f,
            fontFamily = prefs[Keys.FONT_FAMILY]?.toEnumOrNull<NoteFontFamily>() ?: NoteFontFamily.SYSTEM,
            syntaxTheme = prefs[Keys.SYNTAX_THEME]?.toEnumOrNull<SyntaxTheme>() ?: SyntaxTheme.MATERIAL,
            lineSpacing = prefs[Keys.LINE_SPACING]?.toEnumOrNull<EditorLineSpacing>() ?: EditorLineSpacing.NORMAL,
            enabledLanguages = prefs[Keys.ENABLED_LANGUAGES] ?: emptySet()
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setThemeColor(color: AppThemeColor) {
        context.settingsDataStore.edit { it[Keys.THEME_COLOR] = color.name }
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

    suspend fun setFontSizeScale(scale: Float) {
        context.settingsDataStore.edit { it[Keys.FONT_SIZE_SCALE] = scale }
    }

    suspend fun setFontFamily(family: NoteFontFamily) {
        context.settingsDataStore.edit { it[Keys.FONT_FAMILY] = family.name }
    }

    suspend fun setSyntaxTheme(theme: SyntaxTheme) {
        context.settingsDataStore.edit { it[Keys.SYNTAX_THEME] = theme.name }
    }

    suspend fun setLineSpacing(spacing: EditorLineSpacing) {
        context.settingsDataStore.edit { it[Keys.LINE_SPACING] = spacing.name }
    }

    suspend fun setEnabledLanguages(languages: Set<String>) {
        context.settingsDataStore.edit { it[Keys.ENABLED_LANGUAGES] = languages }
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    runCatching { enumValueOf<T>(this) }.getOrNull()

