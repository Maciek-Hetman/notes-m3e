package com.maciejhetman.notes.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.data.AppSettings
import com.maciejhetman.notes.data.EditorLineSpacing
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.data.NoteFontFamily
import com.maciejhetman.notes.data.SyntaxTheme
import com.maciejhetman.notes.data.ThemeMode
import com.maciejhetman.notes.ui.util.tap
import com.maciejhetman.notes.ui.util.toggle
import com.maciejhetman.notes.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val haptics = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptics.tap()
                            onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            SettingsSectionHeader("Preview")
            SettingsLivePreviewCard(settings = settings)

            SettingsSectionHeader("Appearance")
            SettingsCard {
                SettingsMenuRow(
                    title = "Theme",
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    labelFor = ::themeModeLabel,
                    onSelect = viewModel::setThemeMode
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Material You",
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor
                    )
                }

                SettingsDivider()
                SettingsSwitchRow(
                    title = "AMOLED black",
                    checked = settings.amoledBlack,
                    onCheckedChange = viewModel::setAmoledBlack
                )
            }

            SettingsSectionHeader("Typography & Font")
            SettingsCard {
                SettingsMenuRow(
                    title = "Font style",
                    options = NoteFontFamily.entries,
                    selected = settings.fontFamily,
                    labelFor = { it.label },
                    onSelect = viewModel::setFontFamily
                )

                SettingsDivider()

                val fontPercent = (settings.fontSizeScale * 100).roundToInt()
                SettingsSliderRow(
                    title = "Font size",
                    value = settings.fontSizeScale,
                    valueRange = 0.70f..1.50f,
                    valueLabel = "$fontPercent%",
                    onValueChange = viewModel::setFontSizeScale
                )

                SettingsDivider()

                SettingsMenuRow(
                    title = "Line spacing",
                    options = EditorLineSpacing.entries,
                    selected = settings.lineSpacing,
                    labelFor = { it.label },
                    onSelect = viewModel::setLineSpacing
                )
            }

            SettingsSectionHeader("Code & Line Numbers")
            SettingsCard {
                SettingsMenuRow(
                    title = "Syntax highlighting theme",
                    options = SyntaxTheme.entries,
                    selected = settings.syntaxTheme,
                    labelFor = { it.label },
                    onSelect = viewModel::setSyntaxTheme
                )

                SettingsDivider()

                SettingsMenuRow(
                    title = "Line numbers",
                    options = LineNumberMode.entries,
                    selected = settings.lineNumberMode,
                    labelFor = ::lineNumberModeLabel,
                    onSelect = viewModel::setLineNumberMode
                )
            }
        }
    }
}

@Composable
private fun SettingsLivePreviewCard(settings: AppSettings) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val density = LocalDensity.current

    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val syntaxColors = resolveSyntaxThemeColors(
        theme = settings.syntaxTheme,
        isDark = isDark,
        fallbackPrimary = primaryColor,
        fallbackSecondary = secondaryColor,
        fallbackTertiary = tertiaryColor,
        fallbackOnSurface = onSurfaceColor,
        fallbackSurfaceVariant = surfaceVariant
    )

    val gutterWidthDp = (24f * settings.fontSizeScale).dp
    val gutterWidthSp = with(density) { gutterWidthDp.toSp() }

    val sampleMarkdown = "### Preview Note\n" +
            "This is **bold**, *italic*, and `inline code` formatted using your font style.\n\n" +
            "```kotlin\n" +
            "fun main() {\n" +
            "    val greeting = \"Hello World!\"\n" +
            "    println(greeting) // 42\n" +
            "}\n" +
            "```"

    val transformation = remember(settings, primaryColor, onSurfaceColor, surfaceVariant, gutterWidthSp, isDark) {
        MarkdownVisualTransformation(
            primaryColor = primaryColor,
            onSurfaceColor = onSurfaceColor,
            codeBackground = surfaceVariant,
            selection = TextRange(0),
            imageAspectRatios = emptyMap(),
            containerWidthDp = 300f,
            customHighlightColors = syntaxColors,
            fontFamily = settings.fontFamily.toComposeFontFamily(),
            fontScale = settings.fontSizeScale,
            lineNumberMode = settings.lineNumberMode,
            gutterWidth = gutterWidthSp
        )
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val transformedText = transformation.filter(androidx.compose.ui.text.AnnotatedString(sampleMarkdown)).text
            Text(
                text = transformedText,
                fontFamily = settings.fontFamily.toComposeFontFamily(),
                fontSize = (15 * settings.fontSizeScale).sp,
                lineHeight = (15 * settings.fontSizeScale * settings.lineSpacing.multiplier).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun lineNumberModeLabel(mode: LineNumberMode): String = when (mode) {
    LineNumberMode.OFF -> "Off"
    LineNumberMode.ALL_LINES -> "Whole note (all lines)"
    LineNumberMode.CODE_BLOCKS_ONLY -> "Code snippets only"
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember(value) { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
private fun <T> SettingsMenuRow(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable {
                haptics.tap()
                expanded = true
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labelFor(selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(20.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = labelFor(option),
                                fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (option == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingIcon = if (option == selected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null,
                        onClick = {
                            haptics.tap()
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val onToggle: (Boolean) -> Unit = { newChecked ->
        haptics.toggle(newChecked)
        onCheckedChange(newChecked)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

