package com.maciejhetman.notes.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import com.maciejhetman.notes.ui.screens.FENCED_CODE_REGEX
import com.maciejhetman.notes.ui.util.IMAGE_MARKDOWN_REGEX

private val TODO_LINE_REGEX = Regex("(?m)^\\s*[-*] \\[[ xX]\\] ")

/**
 * State holder for the note content editor. Hoisted into [com.maciejhetman.notes.ui.screens.NoteDetailScreen]
 * (via [rememberNoteEditorState]) so the toolbar FAB and photo picker can edit the same field
 * state that [NoteContentEditor] and its overlays render.
 */
@Stable
class NoteEditorState(initialContent: String) {
    // TextFieldValue preserves cursor position for toolbar insertions
    var contentFieldValue by mutableStateOf(TextFieldValue(initialContent))
    var textLayoutResult by mutableStateOf<TextLayoutResult?>(null)
    var activeLanguageMenuBlockIndex by mutableStateOf<Int?>(null)

    // Real aspect ratios of inline images, measured as their painters load — used to reserve
    // the correct height in the visual transformation.
    val imageAspectRatios = mutableStateMapOf<String, Float>()

    // Real rendered width of the content field — used so the reserved height for inline
    // images matches their actual displayed width/aspect-ratio (otherwise they look squashed).
    var containerWidthPx by mutableIntStateOf(0)

    // ── Cached regex matches ──────────────────────────────────────────────
    // These would otherwise re-evaluate on every composition frame inside decorationBox.
    // derivedStateOf recomputes only when the text actually changes.
    val cachedFencedMatches: List<MatchResult> by derivedStateOf {
        FENCED_CODE_REGEX.findAll(contentFieldValue.text).toList()
    }
    val cachedTodoMatches: List<MatchResult> by derivedStateOf {
        TODO_LINE_REGEX.findAll(contentFieldValue.text).toList()
    }
    val cachedImageMatches: List<MatchResult> by derivedStateOf {
        IMAGE_MARKDOWN_REGEX.findAll(contentFieldValue.text).toList()
    }
}

@Composable
fun rememberNoteEditorState(initialContent: String): NoteEditorState {
    return remember { NoteEditorState(initialContent) }
}
