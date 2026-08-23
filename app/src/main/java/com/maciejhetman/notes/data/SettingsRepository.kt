package com.maciejhetman.notes.data

import android.content.Context
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
    CURSIVE("Cursive / Handwriting")
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

enum class IndentGuideColor(val label: String) {
    AUTO("Auto"),
    GRAY("Gray"),
    BLUE("Blue"),
    GREEN("Green"),
    RED("Red"),
    PURPLE("Purple"),
    CYAN("Cyan")
}

enum class IndentGuideStyle(val label: String) {
    OFF("Off"),
    SOLID("Solid"),
    DASHED("Dashed"),
    DOTTED("Dotted")
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
    val enabledLanguages: Set<String> = emptySet(),
    val textIndentDepthSp: Float = 16f,
    val textIndentColor: IndentGuideColor = IndentGuideColor.AUTO,
    val textIndentStyle: IndentGuideStyle = IndentGuideStyle.DASHED,
    val codeIndentDepthSp: Float = 24f,
    val codeIndentColor: IndentGuideColor = IndentGuideColor.GRAY,
    val codeIndentStyle: IndentGuideStyle = IndentGuideStyle.SOLID
)

/** Stored when the user explicitly disables every syntax language. Empty set still means "all". */
const val NO_LANGUAGES_SENTINEL = "__none__"

fun Set<String>.isAllLanguagesEnabled(): Boolean = isEmpty()

fun Set<String>.isNoLanguagesEnabled(): Boolean =
    contains(NO_LANGUAGES_SENTINEL) || this == setOf("")

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
        val TEXT_INDENT_DEPTH_SP = floatPreferencesKey("text_indent_depth_sp")
        val TEXT_INDENT_COLOR = stringPreferencesKey("text_indent_color")
        val TEXT_INDENT_STYLE = stringPreferencesKey("text_indent_style")
        val CODE_INDENT_DEPTH_SP = floatPreferencesKey("code_indent_depth_sp")
        val CODE_INDENT_COLOR = stringPreferencesKey("code_indent_color")
        val CODE_INDENT_STYLE = stringPreferencesKey("code_indent_style")
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
            enabledLanguages = prefs[Keys.ENABLED_LANGUAGES] ?: emptySet(),
            textIndentDepthSp = prefs[Keys.TEXT_INDENT_DEPTH_SP] ?: 16f,
            textIndentColor = prefs[Keys.TEXT_INDENT_COLOR]?.toEnumOrNull<IndentGuideColor>() ?: IndentGuideColor.AUTO,
            textIndentStyle = prefs[Keys.TEXT_INDENT_STYLE]?.toEnumOrNull<IndentGuideStyle>() ?: IndentGuideStyle.DASHED,
            codeIndentDepthSp = prefs[Keys.CODE_INDENT_DEPTH_SP] ?: 24f,
            codeIndentColor = prefs[Keys.CODE_INDENT_COLOR]?.toEnumOrNull<IndentGuideColor>() ?: IndentGuideColor.GRAY,
            codeIndentStyle = prefs[Keys.CODE_INDENT_STYLE]?.toEnumOrNull<IndentGuideStyle>() ?: IndentGuideStyle.SOLID
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

    suspend fun setTextIndentDepthSp(depthSp: Float) {
        context.settingsDataStore.edit { it[Keys.TEXT_INDENT_DEPTH_SP] = depthSp }
    }

    suspend fun setTextIndentColor(color: IndentGuideColor) {
        context.settingsDataStore.edit { it[Keys.TEXT_INDENT_COLOR] = color.name }
    }

    suspend fun setTextIndentStyle(style: IndentGuideStyle) {
        context.settingsDataStore.edit { it[Keys.TEXT_INDENT_STYLE] = style.name }
    }

    suspend fun setCodeIndentDepthSp(depthSp: Float) {
        context.settingsDataStore.edit { it[Keys.CODE_INDENT_DEPTH_SP] = depthSp }
    }

    suspend fun setCodeIndentColor(color: IndentGuideColor) {
        context.settingsDataStore.edit { it[Keys.CODE_INDENT_COLOR] = color.name }
    }

    suspend fun setCodeIndentStyle(style: IndentGuideStyle) {
        context.settingsDataStore.edit { it[Keys.CODE_INDENT_STYLE] = style.name }
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    runCatching { enumValueOf<T>(this) }.getOrNull()

