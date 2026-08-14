package com.maciejhetman.notes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.maciejhetman.notes.data.AppSettings
import com.maciejhetman.notes.data.IndentGuideStyle
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.ui.screens.SUPPORTED_LANGUAGES
import com.maciejhetman.notes.ui.screens.computeIndentGuides
import com.maciejhetman.notes.ui.screens.computeNumberedLines
import com.maciejhetman.notes.ui.screens.resolve
import com.maciejhetman.notes.ui.util.tap

/**
 * Whole-block background containers for fenced code blocks, rendered *behind* the text
 * (i.e. before innerTextField() in the decorationBox).
 */
@Composable
fun EditorCodeBlockBackgrounds(
    state: NoteEditorState,
    backgroundColor: Color
) {
    val density = LocalDensity.current
    state.textLayoutResult?.let { layoutResult ->
        val text = state.contentFieldValue.text
        if (text.isNotEmpty() && layoutResult.layoutInput.text.length == text.length) {
            for (match in state.cachedFencedMatches) {
                val language = match.groupValues[1]
                val content = match.groupValues[2]
                val startOffset = match.range.first.coerceIn(0, text.length - 1)
                if (startOffset >= layoutResult.layoutInput.text.length) continue

                val contentStart = match.range.first + 3 + language.length + 1
                val contentEnd = contentStart + content.length
                val lastContentOffset = (contentEnd - 1).coerceIn(startOffset, (text.length - 1).coerceAtLeast(0))

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
                            color = backgroundColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }
    }
}

/**
 * VSCode-style vertical indent guides. One thin line is drawn per indentation level at
 * x = baseIndent + k * depth, where baseIndent matches the editor's existing line-number gutter
 * and code-block padding. Guides are purely visual and do not move the underlying text, so the
 * depth setting is a user-tunable grid the user can align with their actual whitespace.
 * Drawn *behind* the text (before innerTextField).
 */
@Composable
fun IndentGuideLines(
    layoutResult: TextLayoutResult?,
    text: String,
    appSettings: AppSettings,
    gutterWidthDp: Dp
) {
    if (layoutResult == null) return
    if (text.isEmpty()) return
    if (layoutResult.layoutInput.text.length != text.length) return

    val guides = remember(text) { computeIndentGuides(text) }
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val fontScale = appSettings.fontSizeScale
    val innerPaddingDp = with(density) { (10f * fontScale).sp.toDp() }

    for (guide in guides) {
        if (guide.level <= 0) continue
        val style = if (guide.insideFence) appSettings.codeIndentStyle else appSettings.textIndentStyle
        if (style == IndentGuideStyle.OFF) continue

        val safeOffset = guide.startOffset.coerceIn(0, (text.length - 1).coerceAtLeast(0))
        if (safeOffset >= layoutResult.layoutInput.text.length) continue
        val lineIndex = layoutResult.getLineForOffset(safeOffset)
        val lineTop = layoutResult.getLineTop(lineIndex)
        val lineBottom = layoutResult.getLineBottom(lineIndex)
        if (lineBottom <= lineTop) continue
        val lineHeightDp = with(density) { (lineBottom - lineTop).toDp() }

        val color = (if (guide.insideFence) appSettings.codeIndentColor else appSettings.textIndentColor)
            .resolve(onSurfaceVariant)
        // Use sp for the guide depth so it scales with the text indent (which the transformation
        // applies in sp). The app fontScale is applied here, and the system font scale is applied
        // by .sp.toPx() inside the DrawScope, keeping overlay and text pixel-aligned.
        val depthSp = (if (guide.insideFence) appSettings.codeIndentDepthSp else appSettings.textIndentDepthSp) * fontScale
        val baseDp = when {
            appSettings.lineNumberMode == LineNumberMode.ALL_LINES -> gutterWidthDp + innerPaddingDp
            guide.insideFence && appSettings.lineNumberMode == LineNumberMode.CODE_BLOCKS_ONLY -> gutterWidthDp + innerPaddingDp
            guide.insideFence -> innerPaddingDp
            else -> Dp(0f)
        }
        val pathEffect = when (style) {
            IndentGuideStyle.SOLID -> null
            IndentGuideStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            IndentGuideStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(2f, 6f))
            IndentGuideStyle.OFF -> null
        }

        for (k in 1..guide.level) {
            val xPx = with(density) { baseDp.toPx() + (depthSp * k).sp.toPx() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(xPx.toInt(), lineTop.toInt()) }
                    .width(1.dp)
                    .height(lineHeightDp)
                    .drawBehind {
                        drawLine(
                            color = color,
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = pathEffect
                        )
                    }
            )
        }
    }
}

/**
 * Everything drawn *on top of* the text (after innerTextField() in the decorationBox):
 * line-number gutter, todo checkboxes, code-block language selector menus, inline images.
 */
@Composable
fun EditorOverlays(
    state: NoteEditorState,
    onContentChange: (String) -> Unit,
    contentFocusRequester: FocusRequester,
    appSettings: AppSettings,
    fontScale: Float,
    gutterWidthDp: Dp
) {
    state.textLayoutResult?.let { layoutResult ->
        val text = state.contentFieldValue.text
        if (text.isEmpty()) return@let
        if (layoutResult.layoutInput.text.length != text.length) return@let

        if (appSettings.lineNumberMode != LineNumberMode.OFF) {
            LineNumberGutter(
                layoutResult = layoutResult,
                text = text,
                lineNumberMode = appSettings.lineNumberMode,
                gutterWidthDp = gutterWidthDp,
                fontScale = fontScale
            )
        }

        TodoCheckboxOverlays(state = state, layoutResult = layoutResult)

        LanguageSelectorMenus(
            state = state,
            layoutResult = layoutResult,
            onContentChange = onContentChange,
            enabledLanguages = appSettings.enabledLanguages,
            fontScale = fontScale
        )

        ImageOverlays(
            state = state,
            layoutResult = layoutResult,
            onContentChange = onContentChange,
            contentFocusRequester = contentFocusRequester
        )
    }
}

@Composable
private fun LineNumberGutter(
    layoutResult: TextLayoutResult,
    text: String,
    lineNumberMode: LineNumberMode,
    gutterWidthDp: Dp,
    fontScale: Float
) {
    val density = LocalDensity.current
    val numberedLines = computeNumberedLines(text, lineNumberMode)
    for (numbered in numberedLines) {
        val safeOffset = numbered.startOffset.coerceIn(0, (text.length - 1).coerceAtLeast(0))
        if (safeOffset >= layoutResult.layoutInput.text.length) continue
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
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * fontScale).sp
                ),
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Composable
private fun TodoCheckboxOverlays(
    state: NoteEditorState,
    layoutResult: TextLayoutResult
) {
    val density = LocalDensity.current
    val text = state.contentFieldValue.text
    for (match in state.cachedTodoMatches) {
        val isChecked = match.value.contains("x", ignoreCase = true)
        // We want the bounding box of the '-' character to align perfectly with lists
        val boxStartOffset = match.range.first + match.value.indexOf('-')

        val safeOffset = boxStartOffset.coerceIn(0, (text.length - 1).coerceAtLeast(0))

        // Prevent crash if TextLayoutResult is stale (e.g. immediately after pasting text)
        if (safeOffset >= layoutResult.layoutInput.text.length) continue

        val rect = layoutResult.getBoundingBox(safeOffset)

        // Measure the actual reserved width of the hidden "- [ ] " run (6 chars)
        // so the checkbox can be centered inside it — anchoring only to the left
        // edge left a big, uneven gap before the item text.
        val boxEndOffset = (safeOffset + 6).coerceAtMost(layoutResult.layoutInput.text.length)
        val startX = layoutResult.getHorizontalPosition(safeOffset, true)
        val endX = layoutResult.getHorizontalPosition(boxEndOffset, true)
        val reservedWidthPx = (endX - startX).coerceAtLeast(0f)

        val iconSize = 22.dp
        val iconSizePx = with(density) { iconSize.toPx() }
        // Center vertically but shift down slightly (2.dp) to better align with the text baseline
        val yOffset = rect.top.toInt() + ((rect.height - iconSizePx) / 2).toInt() + with(density) { 2.dp.toPx() }.toInt()
        // Center horizontally within the reserved run instead of hugging the left edge, but
        // bias the centering pool by a trailing gap so the checkbox doesn't crowd the item
        // text immediately to its right. The reserved run itself (see the Todo list branch
        // in MarkdownVisualTransformation) is deliberately wider than the icon so there's
        // real slack here to redistribute rather than this being clamped to zero.
        val checkboxEndPaddingPx = with(density) { 6.dp.toPx() }
        val xOffset = (startX + ((reservedWidthPx - iconSizePx - checkboxEndPaddingPx) / 2).coerceAtLeast(0f)).toInt()

        val icon = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank
        val color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .offset { IntOffset(xOffset, yOffset) }
                .size(iconSize)
        )
    }
}

@Composable
private fun LanguageSelectorMenus(
    state: NoteEditorState,
    layoutResult: TextLayoutResult,
    onContentChange: (String) -> Unit,
    enabledLanguages: Set<String>,
    fontScale: Float
) {
    val density = LocalDensity.current
    val text = state.contentFieldValue.text
    for ((blockIndex, match) in state.cachedFencedMatches.withIndex()) {
        val startOffset = match.range.first.coerceIn(0, (text.length - 1).coerceAtLeast(0))
        if (startOffset >= layoutResult.layoutInput.text.length) continue

        val firstLine = layoutResult.getLineForOffset(startOffset)
        val topPx = layoutResult.getLineTop(firstLine)
        val topPaddingPx = with(density) { 4.dp.toPx() }
        val startY = (topPx - topPaddingPx).coerceAtLeast(0f)

        LanguageSelectorMenu(
            state = state,
            blockIndex = blockIndex,
            match = match,
            startY = startY,
            onContentChange = onContentChange,
            enabledLanguages = enabledLanguages,
            fontScale = fontScale
        )
    }
}

@Composable
private fun LanguageSelectorMenu(
    state: NoteEditorState,
    blockIndex: Int,
    match: MatchResult,
    startY: Float,
    onContentChange: (String) -> Unit,
    enabledLanguages: Set<String>,
    fontScale: Float
) {
    val haptics = LocalHapticFeedback.current
    val language = match.groupValues[1]
    val trimmedLang = language.trim()

    val displayLangName = SUPPORTED_LANGUAGES.firstOrNull {
        it.tag.equals(trimmedLang, ignoreCase = true) || (it.tag.isEmpty() && trimmedLang.isBlank())
    }?.name ?: if (trimmedLang.isNotBlank()) trimmedLang.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } else "Plain text"

    val availableLangs = remember(enabledLanguages, trimmedLang) {
        val list = SUPPORTED_LANGUAGES.filter { lang ->
            enabledLanguages.isEmpty() || enabledLanguages.contains(lang.tag) || lang.tag.equals(trimmedLang, ignoreCase = true)
        }
        if (list.isEmpty()) SUPPORTED_LANGUAGES else list
    }

    val leftIndentDp = 8.dp

    Box(
        modifier = Modifier
            .offset { IntOffset(0, startY.toInt()) }
            .fillMaxWidth()
            .padding(start = leftIndentDp, top = 6.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Box {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 3.dp,
                shadowElevation = 1.dp,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.tap()
                    state.activeLanguageMenuBlockIndex =
                        if (state.activeLanguageMenuBlockIndex == blockIndex) null else blockIndex
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayLangName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = (12.5f * fontScale).sp
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select language",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = state.activeLanguageMenuBlockIndex == blockIndex,
                onDismissRequest = { state.activeLanguageMenuBlockIndex = null },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                availableLangs.forEach { lang ->
                    val isSelected = (lang.tag.equals(trimmedLang, ignoreCase = true)) || (lang.tag.isEmpty() && trimmedLang.isBlank())
                    DropdownMenuItem(
                        text = {
                            Text(
                                lang.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        onClick = {
                            haptics.tap()
                            state.activeLanguageMenuBlockIndex = null

                            val openFenceStart = match.range.first
                            val langStart = openFenceStart + 3
                            val langEnd = langStart + language.length

                            val newText = state.contentFieldValue.text.replaceRange(langStart, langEnd, lang.tag)
                            state.contentFieldValue = TextFieldValue(newText, state.contentFieldValue.selection)
                            onContentChange(newText)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageOverlays(
    state: NoteEditorState,
    layoutResult: TextLayoutResult,
    onContentChange: (String) -> Unit,
    contentFocusRequester: FocusRequester
) {
    val density = LocalDensity.current
    val text = state.contentFieldValue.text
    for (match in state.cachedImageMatches) {
        val isCursorInside = state.contentFieldValue.selection.start in match.range.first..(match.range.last + 1)
        if (isCursorInside) continue

        val safeOffset = match.range.first.coerceIn(0, (text.length - 1).coerceAtLeast(0))
        if (safeOffset >= layoutResult.layoutInput.text.length) continue
        // getBoundingBox: tight ink bounds of the glyph — used only for the y-position.
        val rect = layoutResult.getBoundingBox(safeOffset)
        val yOffset = rect.top.toInt()
        // getCursorRect: full line height (top-of-line → bottom-of-line) reserved by the
        // text engine for the placeholder character. This matches what the visual
        // transformation actually set aside, so the card fills the space exactly.
        val lineHeight = layoutResult.getCursorRect(safeOffset).height
        // Use the measured container width (not text layout width) for full-width images
        val widthDp = with(density) { state.containerWidthPx.toDp() }
        val finalHeight = with(density) { lineHeight.toDp() }

        InlineImageOverlay(
            state = state,
            path = match.groupValues[1],
            matchStart = match.range.first,
            yOffset = yOffset,
            widthDp = widthDp,
            heightDp = finalHeight,
            onContentChange = onContentChange,
            contentFocusRequester = contentFocusRequester
        )
    }
}

@Composable
private fun InlineImageOverlay(
    state: NoteEditorState,
    path: String,
    matchStart: Int,
    yOffset: Int,
    widthDp: Dp,
    heightDp: Dp,
    onContentChange: (String) -> Unit,
    contentFocusRequester: FocusRequester
) {
    val haptics = LocalHapticFeedback.current
    val painter = rememberAsyncImagePainter(model = path)
    val intrinsicSize = painter.intrinsicSize

    // Cache aspect ratio — uses SideEffect to avoid mutating snapshot
    // state during composition (which would violate Compose's contract).
    SideEffect {
        if (intrinsicSize.width > 0 && intrinsicSize.height > 0) {
            val r = intrinsicSize.width / intrinsicSize.height
            if (state.imageAspectRatios[path] != r) {
                state.imageAspectRatios[path] = r
            }
        }
    }

    Card(
        modifier = Modifier
            .offset { IntOffset(0, yOffset) }
            .width(widthDp)
            .height(heightDp)
            .padding(vertical = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                contentFocusRequester.requestFocus()
                state.contentFieldValue = TextFieldValue(
                    state.contentFieldValue.text,
                    TextRange(matchStart)
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                // Crop (not FillBounds) so any small rounding mismatch between the
                // reserved height and the box's actual aspect ratio never stretches
                // the image — it crops slightly instead of squashing it.
                contentScale = ContentScale.Crop
            )
            // Remove image button (small Box to override IconButton min touch target)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(18.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.tap()
                        val pattern = "!\\[.*?\\]\\(${Regex.escape(path)}\\)"
                        val newText = state.contentFieldValue.text.replace(Regex(pattern), "")
                        state.contentFieldValue = TextFieldValue(newText, TextRange(newText.length))
                        onContentChange(newText)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove photo",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
