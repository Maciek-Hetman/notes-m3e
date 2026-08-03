package com.maciejhetman.notes.ui.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

// Compiled once at file level rather than on every keystroke — applyListContinuation runs
// inside the content field's onValueChange.
private val LIST_CONTINUATION_MARKER_REGEX = Regex("^(\\s*)(- \\[[ xX]\\]|[-*]|\\d+\\.)\\s+")
private val ORDERED_LIST_MARKER_REGEX = Regex("^(\\s*)(\\d+)\\.\\s+")
private val CHECKED_TODO_MARKER_REGEX = Regex("- \\[[xX]\\]", RegexOption.IGNORE_CASE)

/**
 * Auto-continues a markdown list when Enter is pressed: bullets/ordered/todo items get their
 * next marker inserted, ordered lists increment, checked todos reset to unchecked, and
 * pressing Enter on an empty item removes the marker instead (ending the list).
 *
 * Pure function — returns [new] unchanged when no continuation applies.
 */
fun applyListContinuation(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    // Check if user pressed Enter (exactly one newline was just inserted right
    // before the cursor). We compare newline *counts* rather than requiring an
    // exact +1 total length diff, since some IMEs bundle autocorrect/autocapitalize
    // edits together with the Enter keystroke — a strict length check would then
    // silently skip this block and leave a stray list marker (e.g. "1.") behind on
    // its own empty line. Requiring exactly one *new* newline (not "at least one")
    // still avoids misfiring on multi-line pastes.
    val addedNewline = new.text.count { it == '\n' } == old.text.count { it == '\n' } + 1 &&
        new.selection.start > 0 &&
        new.text.getOrNull(new.selection.start - 1) == '\n'
    if (!addedNewline) return new

    val textBeforeEnter = new.text.substring(0, new.selection.start - 1)
    val lastLine = textBeforeEnter.substringAfterLast('\n')
    val listMarker = LIST_CONTINUATION_MARKER_REGEX.find(lastLine) ?: return new

    val marker = listMarker.value
    return if (lastLine.length == marker.length) {
        // Empty list item, cancel it by removing the marker
        val text = new.text.removeRange(new.selection.start - 1 - marker.length, new.selection.start - 1)
        TextFieldValue(text, TextRange(new.selection.start - marker.length))
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
        val newText = new.text.substring(0, new.selection.start) + nextMarker + new.text.substring(new.selection.end)
        TextFieldValue(newText, TextRange(new.selection.start + nextMarker.length))
    }
}
