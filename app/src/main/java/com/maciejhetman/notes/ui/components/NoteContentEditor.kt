package com.maciejhetman.notes.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.data.AppSettings
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.ui.screens.CodeHighlightColors
import com.maciejhetman.notes.ui.screens.FENCED_CODE_REGEX
import com.maciejhetman.notes.ui.util.applyListContinuation
import com.maciejhetman.notes.ui.util.tap
import com.maciejhetman.notes.ui.util.toggle

private val TODO_MARKER_REGEX = Regex("- \\[[ xX]\\] ")

/**
 * The note's content field: a [BasicTextField] with live-markdown [VisualTransformation],
 * Enter-key list auto-continuation, tap handling for todo checkboxes and code-block language
 * tags, and cursor-above-keyboard scrolling. Rendered overlays live in EditorOverlays.kt.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteContentEditor(
    state: NoteEditorState,
    onContentChange: (String) -> Unit,
    contentFocusRequester: FocusRequester,
    visualTransformation: VisualTransformation,
    appSettings: AppSettings,
    fontFamily: FontFamily,
    syntaxColors: CodeHighlightColors,
    fallbackCodeBackground: Color,
    gutterWidthDp: Dp
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val fontScale = appSettings.fontSizeScale
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Re-run whenever the IME's height changes (i.e. the keyboard is animating open/closed),
    // not just when the cursor moves — otherwise bringIntoView fires before the keyboard has
    // actually appeared (using the pre-keyboard viewport), scrolling nowhere near far enough
    // to keep the cursor visible above it.
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    LaunchedEffect(state.contentFieldValue.selection, state.textLayoutResult, imeBottomPx) {
        val layoutResult = state.textLayoutResult ?: return@LaunchedEffect
        val selection = state.contentFieldValue.selection
        val cursorStart = selection.start
        if (cursorStart >= 0 && cursorStart <= state.contentFieldValue.text.length) {
            if (cursorStart <= layoutResult.layoutInput.text.length) {
                val cursorRect = layoutResult.getCursorRect(cursorStart)
                // Extra bottom margin roughly matches the floating toolbar's height so the
                // cursor lands just above the toolbar (and keyboard), not tucked right behind it.
                val extraSpacingPx = with(density) { 96.dp.toPx() }
                val extendedRect = cursorRect.copy(
                    bottom = cursorRect.bottom + extraSpacingPx
                )
                bringIntoViewRequester.bringIntoView(extendedRect)
            }
        }
    }

    BasicTextField(
        value = state.contentFieldValue,
        onValueChange = { newVal ->
            val continued = applyListContinuation(state.contentFieldValue, newVal)
            state.contentFieldValue = continued
            onContentChange(continued.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (appSettings.lineNumberMode != LineNumberMode.OFF) 12.dp else 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 8.dp
            )
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(contentFocusRequester)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        // Only trigger on the initial press, not on moves
                        val down = event.changes.firstOrNull { !it.previousPressed && it.pressed }
                        if (down != null) {
                            state.textLayoutResult?.let { layoutResult ->
                                if (state.contentFieldValue.text.isEmpty()) return@let
                                val offset = layoutResult.getOffsetForPosition(down.position)
                                // getOffsetForPosition can return length of string, which is out of bounds for getBoundingBox
                                val safeOffset = offset.coerceAtMost(state.contentFieldValue.text.length - 1)
                                val rect = layoutResult.getBoundingBox(safeOffset)
                                // Ensure tap is visually on the text, not just mapped from empty space
                                val expandedRect = rect.copy(
                                    left = rect.left - 40f,
                                    right = rect.right + 40f,
                                    top = rect.top - 40f,
                                    bottom = rect.bottom + 40f
                                )

                                if (expandedRect.contains(down.position)) {
                                    val codeMatches = FENCED_CODE_REGEX.findAll(state.contentFieldValue.text).toList()
                                    for ((blockIndex, match) in codeMatches.withIndex()) {
                                        val language = match.groupValues[1]
                                        val openFenceStart = match.range.first
                                        val langStart = openFenceStart + 3
                                        val langEnd = langStart + language.length
                                        if (offset in openFenceStart..(langEnd + 1)) {
                                            down.consume()
                                            haptics.tap()
                                            state.activeLanguageMenuBlockIndex =
                                                if (state.activeLanguageMenuBlockIndex == blockIndex) null else blockIndex
                                            break
                                        }
                                    }

                                    val matches = TODO_MARKER_REGEX.findAll(state.contentFieldValue.text)
                                    for (match in matches) {
                                        if (offset in match.range) {
                                            down.consume()
                                            val isChecked = match.value.contains("x", ignoreCase = true)
                                            val replacement = if (isChecked) "- [ ] " else "- [x] "
                                            haptics.toggle(checked = !isChecked)
                                            val newText = state.contentFieldValue.text.replaceRange(match.range, replacement)

                                            state.contentFieldValue = TextFieldValue(newText, state.contentFieldValue.selection)
                                            onContentChange(newText)
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = fontFamily,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * fontScale).sp,
            lineHeight = (MaterialTheme.typography.bodyLarge.fontSize.value * fontScale * appSettings.lineSpacing.multiplier).sp
        ),
        visualTransformation = visualTransformation,
        onTextLayout = { state.textLayoutResult = it },
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.onGloballyPositioned { state.containerWidthPx = it.size.width }) {
                // Code block background containers go behind the text
                EditorCodeBlockBackgrounds(
                    state = state,
                    backgroundColor = syntaxColors.background ?: fallbackCodeBackground
                )

                if (state.contentFieldValue.text.isEmpty()) {
                    Text(
                        "Start writing…",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = fontFamily,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                            fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * fontScale).sp,
                            lineHeight = (MaterialTheme.typography.bodyLarge.fontSize.value * fontScale * appSettings.lineSpacing.multiplier).sp
                        ),
                        modifier = if (appSettings.lineNumberMode != LineNumberMode.OFF) Modifier.padding(start = gutterWidthDp) else Modifier
                    )
                }
                innerTextField()

                EditorOverlays(
                    state = state,
                    onContentChange = onContentChange,
                    contentFocusRequester = contentFocusRequester,
                    appSettings = appSettings,
                    fontScale = fontScale,
                    gutterWidthDp = gutterWidthDp
                )
            }
        }
    )
}
