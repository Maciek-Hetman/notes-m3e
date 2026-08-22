package com.maciejhetman.notes.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.data.AppSettings
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.data.ThemeMode
import com.maciejhetman.notes.ui.screens.FENCED_CODE_REGEX
import com.maciejhetman.notes.ui.screens.MarkdownVisualTransformation
import com.maciejhetman.notes.ui.screens.computeNumberedLines
import com.maciejhetman.notes.ui.screens.resolveSyntaxThemeColors
import com.maciejhetman.notes.ui.theme.isAppDarkTheme
import com.maciejhetman.notes.ui.theme.toComposeFontFamily
import com.maciejhetman.notes.ui.util.tap
import com.maciejhetman.notes.ui.util.toggle

@Composable
fun SettingsLivePreviewCard(settings: AppSettings, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val density = LocalDensity.current

    val isDark = isAppDarkTheme(settings.themeMode)

    val syntaxColors = resolveSyntaxThemeColors(
        theme = settings.syntaxTheme,
        isDark = isDark,
        fallbackPrimary = primaryColor,
        fallbackSecondary = secondaryColor,
        fallbackTertiary = tertiaryColor,
        fallbackOnSurface = onSurfaceColor,
        fallbackSurfaceVariant = surfaceVariant
    )

    val gutterWidthDp = (36f * settings.fontSizeScale).dp
    val gutterWidthSp = with(density) { gutterWidthDp.toSp() }

    val sampleMarkdown = "### Preview Note\n" +
            "This is **bold**, *italic*, and `inline code` formatted using your font style.\n" +
            "- Item one\n" +
            "  - Nested item\n" +
            "\n" +
            "```kotlin\n" +
            "fun main() {\n" +
            "    val greeting = \"Hello World!\"\n" +
            "    println(greeting) // 42\n" +
            "}\n" +
            "```"

    val transformation = remember(settings, primaryColor, onSurfaceColor, surfaceVariant, gutterWidthSp, isDark, syntaxColors) {
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

    // Precompute once per transformation identity — calling filter() during every
    // unrelated recomposition (scroll, layout pass) was a main-thread hotspot while
    // Settings entered with the hierarchical transition.
    val transformedText = remember(transformation, sampleMarkdown) {
        transformation.filter(androidx.compose.ui.text.AnnotatedString(sampleMarkdown)).text
    }

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(320.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            Box {
                textLayoutResult?.let { layoutResult ->
                    if (layoutResult.layoutInput.text.length == sampleMarkdown.length) {
                        val fencedMatches = FENCED_CODE_REGEX.findAll(sampleMarkdown)
                        val codeBgColor = syntaxColors.background ?: surfaceVariant
                        for (match in fencedMatches) {
                            val language = match.groupValues[1]
                            val content = match.groupValues[2]
                            val startOffset = match.range.first.coerceIn(0, sampleMarkdown.length - 1)
                            val contentStart = match.range.first + 3 + language.length + 1
                            val contentEnd = contentStart + content.length
                            val lastContentOffset = (contentEnd - 1).coerceIn(startOffset, sampleMarkdown.length - 1)

                            val firstLine = layoutResult.getLineForOffset(startOffset)
                            val lastLine = layoutResult.getLineForOffset(lastContentOffset)
                            val topPx = layoutResult.getLineTop(firstLine)
                            val bottomPx = layoutResult.getLineBottom(lastLine)
                            val topPaddingPx = with(density) { 4.dp.toPx() }
                            val bottomPaddingPx = with(density) { 8.dp.toPx() }

                            val startY = (topPx - topPaddingPx).coerceAtLeast(0f)
                            val endY = bottomPx + bottomPaddingPx
                            val blockHeightDp = with(density) { (endY - startY).toDp() }

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(0, startY.toInt()) }
                                    .fillMaxWidth()
                                    .height(blockHeightDp)
                                    .background(
                                        color = codeBgColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                            
                            val trimmedLang = language.trim()
                            val displayLangName = com.maciejhetman.notes.ui.screens.SUPPORTED_LANGUAGES.firstOrNull {
                                it.tag.equals(trimmedLang, ignoreCase = true) || (it.tag.isEmpty() && trimmedLang.isBlank())
                            }?.name ?: if (trimmedLang.isNotBlank()) trimmedLang.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } else "Plain text"

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(0, startY.toInt()) }
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, top = 6.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                androidx.compose.material3.Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 3.dp,
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = displayLangName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = (12.5f * settings.fontSizeScale).sp
                                            )
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                IndentGuideLines(
                    layoutResult = textLayoutResult,
                    text = sampleMarkdown,
                    appSettings = settings,
                    gutterWidthDp = gutterWidthDp
                )

                Text(
                    text = transformedText,
                    onTextLayout = { textLayoutResult = it },
                    fontFamily = settings.fontFamily.toComposeFontFamily(),
                    fontSize = (15 * settings.fontSizeScale).sp,
                    lineHeight = (15 * settings.fontSizeScale * settings.lineSpacing.multiplier).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (settings.lineNumberMode != LineNumberMode.OFF) {
                    textLayoutResult?.let { layoutResult ->
                        if (layoutResult.layoutInput.text.length == sampleMarkdown.length) {
                            val numberedLines = computeNumberedLines(sampleMarkdown, settings.lineNumberMode)
                            for (numbered in numberedLines) {
                                val safeOffset = numbered.startOffset.coerceIn(0, (sampleMarkdown.length - 1).coerceAtLeast(0))
                                val lineIndex = layoutResult.getLineForOffset(safeOffset)
                                val lineTop = layoutResult.getLineTop(lineIndex)
                                val lineBottom = layoutResult.getLineBottom(lineIndex)
                                val lineHeightDp = with(density) { (lineBottom - lineTop).toDp() }
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(0, lineTop.toInt()) }
                                        .width(gutterWidthDp)
                                        .height(lineHeightDp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = numbered.number.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                            fontSize = (12 * settings.fontSizeScale).sp
                                        ),
                                        modifier = Modifier.padding(start = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

fun lineNumberModeLabel(mode: LineNumberMode): String = when (mode) {
    LineNumberMode.OFF -> "Off"
    LineNumberMode.ALL_LINES -> "Whole note (all lines)"
    LineNumberMode.CODE_BLOCKS_ONLY -> "Code snippets only"
}

@Composable
fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Column(
        modifier = modifier
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
fun <T> SettingsMenuRow(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
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
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val onToggle: (Boolean) -> Unit = { newChecked ->
        haptics.toggle(newChecked)
        onCheckedChange(newChecked)
    }

    Row(
        modifier = modifier
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

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable {
                haptics.tap()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
