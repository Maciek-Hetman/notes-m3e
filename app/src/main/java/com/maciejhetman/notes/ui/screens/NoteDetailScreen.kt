package com.maciejhetman.notes.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.ui.theme.LocalAppSettings
import com.maciejhetman.notes.ui.util.tap
import com.maciejhetman.notes.ui.util.toggle
import com.maciejhetman.notes.ui.viewmodel.NoteDetailViewModel
import com.maciejhetman.notes.ui.viewmodel.SavedState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// Hoisted to top-level so they're compiled once rather than on every keystroke — these all run
// either inside onValueChange or inside the content field's decorationBox, both of which
// re-execute on every recomposition of the text being edited.
private val LIST_CONTINUATION_MARKER_REGEX = Regex("^(\\s*)(- \\[[ xX]\\]|[-*]|\\d+\\.)\\s+")
private val ORDERED_LIST_MARKER_REGEX = Regex("^(\\s*)(\\d+)\\.\\s+")
private val CHECKED_TODO_MARKER_REGEX = Regex("- \\[[xX]\\]", RegexOption.IGNORE_CASE)
private val TODO_MARKER_REGEX = Regex("- \\[[ xX]\\] ")
private val TODO_LINE_REGEX = Regex("(?m)^\\s*- \\[[ xX]\\] ")
private val IMAGE_MARKDOWN_REGEX = Regex("!\\[.*?\\]\\((.*?)\\)")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val contentFocusRequester = remember { FocusRequester() }
    val imageAspectRatios = remember { mutableStateMapOf<String, Float>() }
    var containerWidthPx by remember { mutableIntStateOf(0) }
    // TextFieldValue preserves cursor position for toolbar insertions
    var contentFieldValue by remember { mutableStateOf(TextFieldValue(uiState.content)) }

    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val savedPath = copyUriToInternalStorage(context, uri)
                if (savedPath != null) {
                    val syntax = "\n![image]($savedPath)\n"
                    val (newValue) = buildInsertedValue(syntax, contentFieldValue)
                    contentFieldValue = newValue
                    viewModel.updateContent(newValue.text)
                    contentFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
        }
    )

    val dateFormatter = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val timeFormatter = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    val createdStr = remember(uiState.createdAt) { dateFormatter.format(java.util.Date(uiState.createdAt)) }
    val modifiedStr = remember(uiState.modifiedAt) { timeFormatter.format(java.util.Date(uiState.modifiedAt)) }

    // Sync with Room on first load (existing note)
    LaunchedEffect(uiState.id) {
        if (contentFieldValue.text != uiState.content) {
            contentFieldValue = TextFieldValue(uiState.content, TextRange(uiState.content.length))
        }
    }

    // "Saved" flash indicator
    var showSavedIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.savedState) {
        if (uiState.savedState == SavedState.Saved) {
            showSavedIndicator = true
            delay(2.seconds)
            showSavedIndicator = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.saveNote() }
    }

    // Colors needed by the visual transformation
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val keywordColor = MaterialTheme.colorScheme.primary
    val stringColor = MaterialTheme.colorScheme.tertiary
    val numberColor = MaterialTheme.colorScheme.secondary

    // User preferences (font size, line numbering) — provided app-wide from Settings.
    val appSettings = LocalAppSettings.current
    val fontScale = appSettings.fontSize.scale
    // Width reserved for line-number digits; kept in both Dp (for the overlay Text below) and
    // Sp (for the transformation's paragraph indent) so the two line up pixel-for-pixel.
    val gutterWidthDp = (32f * fontScale).dp
    val gutterWidthSp = with(density) { gutterWidthDp.toSp() }

    // Real rendered width of the content field — used so the reserved height for inline
    // images matches their actual displayed width/aspect-ratio (otherwise they look squashed).
    val containerWidthDp = with(density) { containerWidthPx.toDp().value }

    // Recreated only when theme colors or user preferences change
    val markdownTransformation = remember(
        primaryColor, onSurfaceColor, codeBackground, contentFieldValue.selection, imageAspectRatios.toMap(),
        containerWidthDp, keywordColor, stringColor, numberColor, fontScale, appSettings.lineNumberMode, gutterWidthSp
    ) {
        MarkdownVisualTransformation(
            primaryColor, onSurfaceColor, codeBackground, contentFieldValue.selection, imageAspectRatios, containerWidthDp,
            keywordColor = keywordColor, stringColor = stringColor, numberColor = numberColor,
            fontScale = fontScale, lineNumberMode = appSettings.lineNumberMode, gutterWidth = gutterWidthSp
        )
    }

    // ── UI ─────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showSavedIndicator,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it }
                    ) {
                        Text(
                            "Saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.saveNote(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            MarkdownToolbar(
                onInsert = { syntax ->
                    val (newValue) = buildInsertedValue(syntax, contentFieldValue)
                    contentFieldValue = newValue
                    viewModel.updateContent(newValue.text)
                    // Toolbar buttons steal focus from the text field, which dismisses the
                    // keyboard — explicitly refocus and re-show it so typing can continue.
                    contentFocusRequester.requestFocus()
                    keyboardController?.show()
                },
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier.imePadding()
            )
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // ── Title ─────────────────────────────────────────────────────
            BasicTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { contentFocusRequester.requestFocus() }
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (uiState.title.isEmpty()) {
                            Text(
                                "Title",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Text(
                text = "Created $createdStr  •  Modified $modifiedStr",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // ── Content — live markdown via VisualTransformation ───────────
            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            val bringIntoViewRequester = remember { BringIntoViewRequester() }

            // Re-run whenever the IME's height changes (i.e. the keyboard is animating open/closed),
            // not just when the cursor moves — otherwise bringIntoView fires before the keyboard has
            // actually appeared (using the pre-keyboard viewport), scrolling nowhere near far enough
            // to keep the cursor visible above it.
            val imeBottomPx = WindowInsets.ime.getBottom(density)

            LaunchedEffect(contentFieldValue.selection, textLayoutResult, imeBottomPx) {
                val layoutResult = textLayoutResult ?: return@LaunchedEffect
                val selection = contentFieldValue.selection
                val cursorStart = selection.start
                if (cursorStart >= 0 && cursorStart <= contentFieldValue.text.length) {
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
                    value = contentFieldValue,
                    onValueChange = { newVal ->
                        var newTextFieldValue = newVal
                        
                        // Check if user pressed Enter (exactly one newline was just inserted right
                        // before the cursor). We compare newline *counts* rather than requiring an
                        // exact +1 total length diff, since some IMEs bundle autocorrect/autocapitalize
                        // edits together with the Enter keystroke — a strict length check would then
                        // silently skip this block and leave a stray list marker (e.g. "1.") behind on
                        // its own empty line. Requiring exactly one *new* newline (not "at least one")
                        // still avoids misfiring on multi-line pastes.
                        val addedNewline = newVal.text.count { it == '\n' } == contentFieldValue.text.count { it == '\n' } + 1 &&
                            newVal.selection.start > 0 &&
                            newVal.text.getOrNull(newVal.selection.start - 1) == '\n'
                        if (addedNewline) {
                            
                            val textBeforeEnter = newVal.text.substring(0, newVal.selection.start - 1)
                            val lastLine = textBeforeEnter.substringAfterLast('\n')
                            val listMarker = LIST_CONTINUATION_MARKER_REGEX.find(lastLine)
                            
                            if (listMarker != null) {
                                val marker = listMarker.value
                                if (lastLine.length == marker.length) {
                                    // Empty list item, cancel it by removing the marker
                                    val text = newVal.text.removeRange(newVal.selection.start - 1 - marker.length, newVal.selection.start - 1)
                                    newTextFieldValue = TextFieldValue(text, TextRange(newVal.selection.start - marker.length))
                                } else {
                                    // Auto-continue the list
                                    var nextMarker = marker
                                    val numMatch = ORDERED_LIST_MARKER_REGEX.find(marker)
                                    if (numMatch != null) {
                                        val space = numMatch.groupValues[1]
                                        val num = numMatch.groupValues[2].toInt()
                                        nextMarker = "$space${num + 1}. "
                                    } else if (marker.contains("- [x]", ignoreCase = true) || marker.contains("- [X]", ignoreCase = true)) {
                                        nextMarker = marker.replace(CHECKED_TODO_MARKER_REGEX, "- [ ]")
                                    }
                                    val newText = newVal.text.substring(0, newVal.selection.start) + nextMarker + newVal.text.substring(newVal.selection.end)
                                    newTextFieldValue = TextFieldValue(newText, TextRange(newVal.selection.start + nextMarker.length))
                                }
                            }
                        }
                        
                        contentFieldValue = newTextFieldValue
                        viewModel.updateContent(newTextFieldValue.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .focusRequester(contentFocusRequester)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    // Only trigger on the initial press, not on moves
                                    val down = event.changes.firstOrNull { !it.previousPressed && it.pressed }
                                    if (down != null) {
                                        textLayoutResult?.let { layoutResult ->
                                            if (contentFieldValue.text.isEmpty()) return@let
                                            val offset = layoutResult.getOffsetForPosition(down.position)
                                            // getOffsetForPosition can return length of string, which is out of bounds for getBoundingBox
                                            val safeOffset = offset.coerceAtMost(contentFieldValue.text.length - 1)
                                            val rect = layoutResult.getBoundingBox(safeOffset)
                                            // Ensure tap is visually on the text, not just mapped from empty space
                                            val expandedRect = rect.copy(
                                                left = rect.left - 40f,
                                                right = rect.right + 40f,
                                                top = rect.top - 40f,
                                                bottom = rect.bottom + 40f
                                            )
                                            
                                            if (expandedRect.contains(down.position)) {
                                                val matches = TODO_MARKER_REGEX.findAll(contentFieldValue.text)
                                                for (match in matches) {
                                                    if (offset in match.range) {
                                                        down.consume()
                                                        val isChecked = match.value.contains("x", ignoreCase = true)
                                                        val replacement = if (isChecked) "- [ ] " else "- [x] "
                                                        haptics.toggle(checked = !isChecked)
                                                        val newText = contentFieldValue.text.replaceRange(match.range, replacement)
                                                        
                                                        val newValLocal = TextFieldValue(newText, contentFieldValue.selection)
                                                        contentFieldValue = newValLocal
                                                        viewModel.updateContent(newText)
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * fontScale).sp,
                        // Explicitly Unspecified (not just "not overridden") — bodyLarge itself carries a
                        // baked-in lineHeight, and ANY fixed lineHeight forces every line in this field to
                        // that exact height, clamping lines that contain an oversized run back down. That
                        // was silently capping our transparent image-placeholder character (and, to a lesser
                        // extent, headings) to a tiny line height no matter how big we made the placeholder.
                        // Leaving it Unspecified lets each line size naturally to its tallest run.
                        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                    ),
                    visualTransformation = markdownTransformation,
                    onTextLayout = { textLayoutResult = it },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.onGloballyPositioned { containerWidthPx = it.size.width }) {
                            if (contentFieldValue.text.isEmpty()) {
                                Text(
                                    "Start writing…",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                        fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * fontScale).sp,
                                        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                                    )
                                )
                            }
                            innerTextField()
                            
                            textLayoutResult?.let { layoutResult ->
                                if (contentFieldValue.text.isEmpty()) return@let

                                if (appSettings.lineNumberMode != LineNumberMode.OFF) {
                                    val numberedLines = computeNumberedLines(contentFieldValue.text, appSettings.lineNumberMode)
                                    for (numbered in numberedLines) {
                                        val safeOffset = numbered.startOffset.coerceIn(0, (contentFieldValue.text.length - 1).coerceAtLeast(0))
                                        if (safeOffset >= layoutResult.layoutInput.text.length) continue
                                        val lineIndex = layoutResult.getLineForOffset(safeOffset)
                                        val top = layoutResult.getLineTop(lineIndex)
                                        Text(
                                            text = numbered.number.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                                fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * fontScale).sp
                                            ),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                            modifier = Modifier
                                                .offset { IntOffset(0, top.toInt()) }
                                                .width(gutterWidthDp)
                                                .padding(end = 6.dp)
                                        )
                                    }
                                }

                                val matches = TODO_LINE_REGEX.findAll(contentFieldValue.text)
                                for (match in matches) {
                                    val isChecked = match.value.contains("x", ignoreCase = true)
                                    // We want the bounding box of the '-' character to align perfectly with lists
                                    val boxStartOffset = match.range.first + match.value.indexOf('-')
                                    
                                    val safeOffset = boxStartOffset.coerceIn(0, (contentFieldValue.text.length - 1).coerceAtLeast(0))
                                    
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

                                // Inline Image Overlays
                                val imageMatches = IMAGE_MARKDOWN_REGEX.findAll(contentFieldValue.text)
                                for (match in imageMatches) {
                                    val isCursorInside = contentFieldValue.selection.start in match.range.first..(match.range.last + 1)
                                    if (!isCursorInside) {
                                        val safeOffset = match.range.first.coerceIn(0, (contentFieldValue.text.length - 1).coerceAtLeast(0))
                                        if (safeOffset >= layoutResult.layoutInput.text.length) continue
                                        // getBoundingBox: tight ink bounds of the glyph — used only for the y-position.
                                        val rect = layoutResult.getBoundingBox(safeOffset)
                                        val yOffset = rect.top.toInt()
                                        // getCursorRect: full line height (top-of-line → bottom-of-line) reserved by the
                                        // text engine for the placeholder character. This matches what the visual
                                        // transformation actually set aside, so the card fills the space exactly.
                                        val lineHeight = layoutResult.getCursorRect(safeOffset).height
                                        val path = match.groupValues[1]
                                        // Use the measured container width (not text layout width) for full-width images
                                        val widthDp = with(density) { containerWidthPx.toDp() }
                                        val painter = rememberAsyncImagePainter(model = path)
                                        val intrinsicSize = painter.intrinsicSize
                                        
                                        // Cache aspect ratio inside state map to dynamically update transformation
                                        if (intrinsicSize.width > 0 && intrinsicSize.height > 0) {
                                            val r = intrinsicSize.width / intrinsicSize.height
                                            if (imageAspectRatios[path] != r) {
                                                imageAspectRatios[path] = r
                                            }
                                        }

                                        val finalHeight = with(density) { lineHeight.toDp() }

                                        Card(
                                            modifier = Modifier
                                                .offset { IntOffset(0, yOffset) }
                                                .width(widthDp)
                                                .height(finalHeight)
                                                .padding(vertical = 4.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    contentFocusRequester.requestFocus()
                                                    contentFieldValue = TextFieldValue(
                                                        contentFieldValue.text,
                                                        TextRange(match.range.first)
                                                    )
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                androidx.compose.foundation.Image(
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
                                                            val newText = contentFieldValue.text.replace(Regex(pattern), "")
                                                            contentFieldValue = TextFieldValue(newText, TextRange(newText.length))
                                                            viewModel.updateContent(newText)
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
                                }
                            }
                        }
                    }
                )

            // Tapping the empty area below the content moves cursor to the end
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        contentFocusRequester.requestFocus()
                        contentFieldValue = TextFieldValue(
                            contentFieldValue.text,
                            TextRange(contentFieldValue.text.length)
                        )
                    }
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Toolbar insertion logic ────────────────────────────────────────────────────

private data class InsertionResult(val newValue: TextFieldValue)

private fun buildInsertedValue(syntax: String, current: TextFieldValue): InsertionResult {
    val sel = current.selection
    val text = current.text
    val selectedText = if (sel.start != sel.end) text.substring(sel.start, sel.end) else ""

    val (insertion, cursorOffset) = when (syntax) {
        "h1"        -> if (selectedText.isNotEmpty()) "# $selectedText" to 0 else "# " to 2
        "h2"        -> if (selectedText.isNotEmpty()) "## $selectedText" to 0 else "## " to 3
        "h3"        -> if (selectedText.isNotEmpty()) "### $selectedText" to 0 else "### " to 4
        "h4"        -> if (selectedText.isNotEmpty()) "#### $selectedText" to 0 else "#### " to 5
        "bold"      -> if (selectedText.isNotEmpty()) "**$selectedText**" to 0 else "****" to 2
        "italic"    -> if (selectedText.isNotEmpty()) "*$selectedText*" to 0 else "**" to 1
        "underline" -> if (selectedText.isNotEmpty()) "<u>$selectedText</u>" to 0 else "<u></u>" to 3
        "code"      -> if (selectedText.isNotEmpty()) "`$selectedText`" to 0 else "``" to 1
        "ul"        -> if (sel.start == 0 || text.getOrNull(sel.start - 1) == '\n') "- " to 2 else "\n- " to 3
        "ol"        -> if (sel.start == 0 || text.getOrNull(sel.start - 1) == '\n') "1. " to 3 else "\n1. " to 4
        "todo"      -> if (sel.start == 0 || text.getOrNull(sel.start - 1) == '\n') "- [ ] " to 6 else "\n- [ ] " to 7
        "quote"     -> if (sel.start == 0 || text.getOrNull(sel.start - 1) == '\n') "> " to 2 else "\n> " to 3
        "hr"        -> "\n---\n" to 5
        else        -> syntax to syntax.length
    }

    val newText = if (selectedText.isNotEmpty()) {
        text.substring(0, sel.start) + insertion + text.substring(sel.end)
    } else {
        text.substring(0, sel.start) + insertion + text.substring(sel.start)
    }
    val newCursor = if (selectedText.isNotEmpty()) sel.start + insertion.length
                    else sel.start + cursorOffset

    return InsertionResult(TextFieldValue(newText, TextRange(newCursor)))
}

// ── Toolbar UI ─────────────────────────────────────────────────────────────────

private enum class ToolbarState { Main, Headings, Lists }

@Composable
private fun MarkdownToolbar(
    onInsert: (String) -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(ToolbarState.Main) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(50),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        AnimatedContent(
            targetState = state,
            label = "toolbar_state"
        ) { targetState ->
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (targetState) {
                    ToolbarState.Main -> {
                        ToolbarIconButton(Icons.Default.Title, "Headings") { state = ToolbarState.Headings }
                        ToolbarDivider()
                        ToolbarIconButton(Icons.Default.FormatBold, "Bold") { onInsert("bold") }
                        ToolbarIconButton(Icons.Default.FormatItalic, "Italic") { onInsert("italic") }
                        ToolbarIconButton(Icons.Default.FormatUnderlined, "Underline") { onInsert("underline") }
                        ToolbarDivider()
                        ToolbarIconButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Lists") { state = ToolbarState.Lists }
                        ToolbarDivider()
                        ToolbarIconButton(Icons.Default.Image, "Insert Photo") { onPickPhoto() }
                    }
                    ToolbarState.Headings -> {
                        ToolbarIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { state = ToolbarState.Main }
                        ToolbarDivider()
                        ToolbarTextButton("H1") { onInsert("h1"); state = ToolbarState.Main }
                        ToolbarTextButton("H2") { onInsert("h2"); state = ToolbarState.Main }
                        ToolbarTextButton("H3") { onInsert("h3"); state = ToolbarState.Main }
                        ToolbarTextButton("H4") { onInsert("h4"); state = ToolbarState.Main }
                    }
                    ToolbarState.Lists -> {
                        ToolbarIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { state = ToolbarState.Main }
                        ToolbarDivider()
                        ToolbarIconButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet") { onInsert("ul"); state = ToolbarState.Main }
                        ToolbarIconButton(Icons.Default.FormatListNumbered, "Numbered") { onInsert("ol"); state = ToolbarState.Main }
                        ToolbarIconButton(Icons.Default.Checklist, "Todo") { onInsert("todo"); state = ToolbarState.Main }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    FilledTonalIconButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ToolbarTextButton(
    text: String,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    FilledTonalIconButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun ToolbarDivider() {
    VerticalDivider(
        modifier = Modifier
            .height(28.dp)
            .padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

private fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
