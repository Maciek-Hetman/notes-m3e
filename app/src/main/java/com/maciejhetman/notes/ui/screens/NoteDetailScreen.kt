package com.maciejhetman.notes.ui.screens

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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
    var showDeleteDialog by remember { mutableStateOf(false) }

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

    // ── Delete confirmation ────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted and cannot be recovered.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteNote()
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

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
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            MarkdownToolbar(
                onInsert = { syntax ->
                    val (newValue) = buildInsertedValue(syntax, contentFieldValue)
                    contentFieldValue = newValue
                    viewModel.updateContent(newValue.text)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
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

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // ── Content — live markdown via VisualTransformation ───────────
            BasicTextField(
                value = contentFieldValue,
                onValueChange = { newVal ->
                    contentFieldValue = newVal
                    viewModel.updateContent(newVal.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp
                ),
                visualTransformation = markdownTransformation,
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
        "h1"     -> if (selectedText.isNotEmpty()) "# $selectedText" to 0 else "# " to 2
        "h2"     -> if (selectedText.isNotEmpty()) "## $selectedText" to 0 else "## " to 3
        "bold"   -> if (selectedText.isNotEmpty()) "**$selectedText**" to 0 else "****" to 2
        "italic" -> if (selectedText.isNotEmpty()) "*$selectedText*" to 0 else "**" to 1
        "code"   -> if (selectedText.isNotEmpty()) "`$selectedText`" to 0 else "``" to 1
        "ul"     -> if (sel.start == 0 || text.getOrNull(sel.start - 1) == '\n') "- " to 2 else "\n- " to 3
        "quote"  -> if (sel.start == 0 || text.getOrNull(sel.start - 1) == '\n') "> " to 2 else "\n> " to 3
        "hr"     -> "\n---\n" to 5
        else     -> syntax to syntax.length
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

@Composable
private fun MarkdownToolbar(
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconButton(Icons.Default.LooksOne,                        "H1")    { onInsert("h1") }
            ToolbarIconButton(Icons.Default.LooksTwo,                        "H2")    { onInsert("h2") }
            ToolbarDivider()
            ToolbarIconButton(Icons.Default.FormatBold,                      "Bold")  { onInsert("bold") }
            ToolbarIconButton(Icons.Default.FormatItalic,                    "Italic"){ onInsert("italic") }
            ToolbarIconButton(Icons.Default.Code,                            "Code")  { onInsert("code") }
            ToolbarDivider()
            ToolbarIconButton(Icons.AutoMirrored.Filled.FormatListBulleted,  "List")  { onInsert("ul") }
            ToolbarIconButton(Icons.Default.FormatQuote,                     "Quote") { onInsert("quote") }
            ToolbarIconButton(Icons.Default.HorizontalRule,                  "Rule")  { onInsert("hr") }
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
private fun ToolbarDivider() {
    VerticalDivider(
        modifier = Modifier
            .height(28.dp)
            .padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
