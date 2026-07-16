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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.ui.viewmodel.NoteDetailViewModel
import com.maciejhetman.notes.ui.viewmodel.SavedState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val dateFormatter = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val timeFormatter = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    val createdStr = remember(uiState.createdAt) { dateFormatter.format(java.util.Date(uiState.createdAt)) }
    val modifiedStr = remember(uiState.modifiedAt) { timeFormatter.format(java.util.Date(uiState.modifiedAt)) }


    // TextFieldValue preserves cursor position for toolbar insertions
    var contentFieldValue by remember { mutableStateOf(TextFieldValue(uiState.content)) }

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
            delay(2000)
            showSavedIndicator = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.saveNote() }
    }

    // Colours needed by the visual transformation
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant

    // Recreated only when theme colours change
    val markdownTransformation = remember(primaryColor, onSurfaceColor, codeBackground) {
        MarkdownVisualTransformation(primaryColor, onSurfaceColor, codeBackground)
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
                }
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
            val density = LocalDensity.current

            BasicTextField(
                value = contentFieldValue,
                onValueChange = { newVal ->
                    var newTextFieldValue = newVal
                    
                    // Check if user pressed Enter (one newline inserted at cursor)
                    if (newVal.text.length - contentFieldValue.text.length == 1 && 
                        newVal.selection.start > 0 &&
                        newVal.text.getOrNull(newVal.selection.start - 1) == '\n') {
                        
                        val textBeforeEnter = newVal.text.substring(0, newVal.selection.start - 1)
                        val lastLine = textBeforeEnter.substringAfterLast('\n')
                        val listMarker = Regex("^(\\s*)(- \\[[ xX]\\]|[-*]|\\d+\\.)\\s+").find(lastLine)
                        
                        if (listMarker != null) {
                            val marker = listMarker.value
                            if (lastLine.length == marker.length) {
                                // Empty list item, cancel it by removing the marker
                                val text = newVal.text.removeRange(newVal.selection.start - 1 - marker.length, newVal.selection.start - 1)
                                newTextFieldValue = TextFieldValue(text, TextRange(newVal.selection.start - marker.length))
                            } else {
                                // Auto-continue the list
                                var nextMarker = marker
                                val numMatch = Regex("^(\\s*)(\\d+)\\.\\s+").find(marker)
                                if (numMatch != null) {
                                    val space = numMatch.groupValues[1]
                                    val num = numMatch.groupValues[2].toInt()
                                    nextMarker = "$space${num + 1}. "
                                } else if (marker.contains("- [x]", ignoreCase = true) || marker.contains("- [X]", ignoreCase = true)) {
                                    nextMarker = marker.replace(Regex("- \\[[xX]\\]", RegexOption.IGNORE_CASE), "- [ ]")
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
                                            val matches = Regex("- \\[[ xX]\\] ").findAll(contentFieldValue.text)
                                            for (match in matches) {
                                                if (offset in match.range) {
                                                    down.consume()
                                                    val isChecked = match.value.contains("x", ignoreCase = true)
                                                    val replacement = if (isChecked) "- [ ] " else "- [x] "
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
                    lineHeight = 28.sp
                ),
                visualTransformation = markdownTransformation,
                onTextLayout = { textLayoutResult = it },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (contentFieldValue.text.isEmpty()) {
                            Text(
                                "Start writing…",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                    lineHeight = 28.sp
                                )
                            )
                        }
                        innerTextField()
                        
                        textLayoutResult?.let { layoutResult ->
                            if (contentFieldValue.text.isEmpty()) return@let
                            val matches = Regex("(?m)^\\s*- \\[[ xX]\\] ").findAll(contentFieldValue.text)
                            for (match in matches) {
                                val isChecked = match.value.contains("x", ignoreCase = true)
                                // We want the bounding box of the '-' character to align perfectly with lists
                                val boxStartOffset = match.range.first + match.value.indexOf('-')
                                
                                val safeOffset = boxStartOffset.coerceIn(0, (contentFieldValue.text.length - 1).coerceAtLeast(0))
                                
                                // Prevent crash if TextLayoutResult is stale (e.g. immediately after pasting text)
                                if (safeOffset >= layoutResult.layoutInput.text.length) continue
                                
                                val rect = layoutResult.getBoundingBox(safeOffset)
                                
                                val iconSize = 21.dp
                                val iconSizePx = with(density) { iconSize.toPx() }
                                // Center vertically but shift down slightly (2.dp) to better align with the text baseline
                                val yOffset = rect.top.toInt() + ((rect.height - iconSizePx) / 2).toInt() + with(density) { 2.dp.toPx() }.toInt()
                                // Align exactly with where the bullet hyphen starts
                                val xOffset = rect.left.toInt()
                                
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
                    }
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
    FilledTonalIconButton(
        onClick = onClick,
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
    FilledTonalIconButton(
        onClick = onClick,
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
