package com.maciejhetman.notes.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

// ─── Inline styles (bold, italic, code) ─────────────────────────────────────

private fun parseInline(text: String, codeBackground: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // Inline code: `code`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            background = codeBackground,
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i++])
                }
            }
            // Bold + italic: ***text***
            // startsWith(prefix, index) avoids allocating a temporary substring just to compare
            // it against a literal, unlike substring(...) == "..." — this runs on every character
            // of every rendered line, so the allocation adds up on longer notes.
            text.startsWith("***", i) -> {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 3, end))
                    }
                    i = end + 3
                } else {
                    append(text[i++])
                }
            }
            // Bold: **text**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i++])
                }
            }
            // Italic: *text* (but not **)
            text[i] == '*' && (i + 1 >= text.length || text[i + 1] != '*') -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && (end + 1 >= text.length || text[end + 1] != '*')) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i++])
                }
            }
            // Italic with _
            text[i] == '_' && (i + 1 >= text.length || text[i + 1] != '_') -> {
                val end = text.indexOf('_', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i++])
                }
            }
            // Underline: <u>text</u> (inserted by the formatting toolbar)
            text.startsWith("<u>", i) -> {
                val end = text.indexOf("</u>", i + 3)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(text.substring(i + 3, end))
                    }
                    i = end + 4
                } else {
                    append(text[i++])
                }
            }
            else -> append(text[i++])
        }
    }
}

// ─── Markdown → rendered preview (for note cards) ────────────────────────────

private val LINK_REGEX = Regex("\\[(.*?)\\]\\(.*?\\)")
private val HEADING_PREFIX_REGEX = Regex("^#{1,6}\\s+")
private val QUOTE_PREFIX_REGEX = Regex("^>\\s+")
private val CHECKED_TODO_PREFIX_REGEX = Regex("^[-*]\\s+\\[[xX]\\]\\s+")
private val UNCHECKED_TODO_PREFIX_REGEX = Regex("^[-*]\\s+\\[ \\]\\s+")
private val UNORDERED_PREFIX_REGEX = Regex("^[-*]\\s+")
private val HR_LINE_REGEX = Regex("^(-{3,}|\\*{3,}|_{3,})$")
private val PREVIEW_IMAGE_LINE_REGEX = Regex("^!\\[.*?\\]\\(.*?\\)$")

/**
 * Strips block-level markdown syntax (headings, list/quote/todo markers, fenced code
 * delimiters, images, horizontal rules) from [text], leaving plain lines with any inline
 * syntax (bold/italic/code/underline) still intact so it can be rendered by [parseInline].
 */
private fun stripBlockSyntaxForPreview(text: String): String {
    val lines = text.lines()
    val result = StringBuilder()
    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        when {
            trimmed.startsWith("```") -> {
                // Skip the block entirely — a code snippet rarely makes for a useful preview
                // and keeping it would drag raw, unhighlighted code into the card.
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) i++
                i++
            }
            trimmed.isEmpty() || trimmed.matches(HR_LINE_REGEX) || PREVIEW_IMAGE_LINE_REGEX.matches(trimmed) -> {
                i++
            }
            else -> {
                val line = when {
                    CHECKED_TODO_PREFIX_REGEX.containsMatchIn(trimmed) ->
                        "☑ " + trimmed.replaceFirst(CHECKED_TODO_PREFIX_REGEX, "")
                    UNCHECKED_TODO_PREFIX_REGEX.containsMatchIn(trimmed) ->
                        "☐ " + trimmed.replaceFirst(UNCHECKED_TODO_PREFIX_REGEX, "")
                    UNORDERED_PREFIX_REGEX.containsMatchIn(trimmed) ->
                        "• " + trimmed.replaceFirst(UNORDERED_PREFIX_REGEX, "")
                    QUOTE_PREFIX_REGEX.containsMatchIn(trimmed) ->
                        trimmed.replaceFirst(QUOTE_PREFIX_REGEX, "")
                    HEADING_PREFIX_REGEX.containsMatchIn(trimmed) ->
                        trimmed.replaceFirst(HEADING_PREFIX_REGEX, "")
                    else -> trimmed
                }.replace(LINK_REGEX, "$1") // Keep link text, drop the URL
                  .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "") // Strip inline images too

                if (result.isNotEmpty()) result.append('\n')
                result.append(line)
                i++
            }
        }
    }
    return result.toString()
}

/**
 * Builds a short, rendered preview of [markdown] suitable for a note card: block syntax
 * (headings, lists, quotes, images, code fences…) is stripped or simplified, while inline
 * styling (bold, italic, inline code, underline) is preserved as real formatting rather than
 * showing raw syntax like `**bold**` or `<u>underlined</u>`.
 */
fun buildNotePreview(markdown: String, codeBackground: Color): AnnotatedString {
    val cleaned = stripBlockSyntaxForPreview(markdown)
    return parseInline(cleaned, codeBackground)
}
