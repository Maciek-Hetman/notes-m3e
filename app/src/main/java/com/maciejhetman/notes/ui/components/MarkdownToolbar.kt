package com.maciejhetman.notes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.ui.util.tap

// ── Toolbar insertion logic ────────────────────────────────────────────────────

data class InsertionResult(val newValue: TextFieldValue)

fun buildInsertedValue(syntax: String, current: TextFieldValue): InsertionResult {
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
        "codeblock" -> {
            val prefix = if (sel.start == 0 || text.getOrNull(sel.start - 1) == '\n') "" else "\n"
            if (selectedText.isNotEmpty()) {
                "${prefix}```\n$selectedText\n```\n" to (prefix.length + 4 + selectedText.length)
            } else {
                "${prefix}```\n\n```\n" to (prefix.length + 4)
            }
        }
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

enum class ToolbarState { Main, Headings, Formatting, Lists }

@Composable
fun MarkdownToolbar(
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
                        ToolbarIconButton(Icons.Default.FormatBold, "Formatting") { state = ToolbarState.Formatting }
                        ToolbarDivider()
                        ToolbarIconButton(Icons.Default.Code, "Code Block") { onInsert("codeblock") }
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
                    ToolbarState.Formatting -> {
                        ToolbarIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { state = ToolbarState.Main }
                        ToolbarDivider()
                        ToolbarIconButton(Icons.Default.FormatBold, "Bold") { onInsert("bold"); state = ToolbarState.Main }
                        ToolbarIconButton(Icons.Default.FormatItalic, "Italic") { onInsert("italic"); state = ToolbarState.Main }
                        ToolbarIconButton(Icons.Default.FormatUnderlined, "Underline") { onInsert("underline"); state = ToolbarState.Main }
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
fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    FilledTonalIconButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier.size(40.dp),
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
fun ToolbarTextButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    FilledTonalIconButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier.size(40.dp),
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
fun ToolbarDivider(modifier: Modifier = Modifier) {
    VerticalDivider(
        modifier = modifier
            .height(28.dp)
            .padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
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
