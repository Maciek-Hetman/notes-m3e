package com.maciejhetman.notes.ui.screens

import androidx.compose.ui.graphics.Color
import com.maciejhetman.notes.data.IndentGuideColor

/** Number of leading-whitespace columns that make up one indentation level. */
const val INDENT_COLUMNS = 4

/** A tab character counts as this many columns of indentation. */
const val TAB_WIDTH = 4

/** Upper bound on how many guide lines a single line can draw (perf guard). */
const val MAX_INDENT_LEVELS = 8

/**
 * A single line and its indent-guide metadata. Shared by the VisualTransformation (which shifts
 * the line's text using ParagraphStyle(TextIndent(...))) and the overlay that draws the guide
 * lines, so the two stay perfectly in sync.
 */
data class IndentGuideLine(
    val startOffset: Int,
    val endOffsetExclusive: Int,
    val leadingWhitespaceLength: Int,
    val level: Int,
    val insideFence: Boolean
)

/**
 * Computes the indent level of every line in [text], based purely on leading whitespace
 * (tabs expand to [TAB_WIDTH] columns, one level = [INDENT_COLUMNS] columns). Lines inside a
 * fenced code block are flagged so the code-block config can be applied to them.
 */
fun computeIndentGuides(text: String): List<IndentGuideLine> {
    if (text.isEmpty()) return emptyList()
    val fences = FENCED_CODE_REGEX.findAll(text).toList()

    fun isInsideFence(start: Int): Boolean = fences.any { fence ->
        val language = fence.groupValues[1]
        val content = fence.groupValues[2]
        val contentStart = fence.range.first + 3 + language.length + 1
        val contentEnd = contentStart + content.length
        start >= contentStart && start < contentEnd
    }

    val result = mutableListOf<IndentGuideLine>()
    var offset = 0
    for (line in text.split('\n')) {
        val lineEnd = (offset + line.length).coerceAtMost(text.length)
        var columns = 0
        var whitespaceLength = 0
        for (c in line) {
            when (c) {
                ' ' -> columns += 1
                '\t' -> columns += TAB_WIDTH
                else -> break
            }
            whitespaceLength++
        }
        val level = if (columns > 0) {
            ((columns + INDENT_COLUMNS - 1) / INDENT_COLUMNS).coerceAtMost(MAX_INDENT_LEVELS)
        } else {
            0
        }
        result += IndentGuideLine(offset, lineEnd, whitespaceLength, level, isInsideFence(offset))
        offset += line.length + 1
    }
    return result
}

/**
 * Resolves an [IndentGuideColor] to a concrete [Color]. [AUTO] follows the theme's
 * [onSurfaceVariant] so it works in both light and dark themes; the fixed presets are
 * mid-brightness hues that stay legible on either background.
 */
fun IndentGuideColor.resolve(onSurfaceVariant: Color): Color = when (this) {
    IndentGuideColor.AUTO -> onSurfaceVariant.copy(alpha = 0.55f)
    IndentGuideColor.GRAY -> Color(0xFF8A8F98)
    IndentGuideColor.BLUE -> Color(0xFF4A90D9)
    IndentGuideColor.GREEN -> Color(0xFF43A047)
    IndentGuideColor.RED -> Color(0xFFE53935)
    IndentGuideColor.PURPLE -> Color(0xFFAB47BC)
    IndentGuideColor.CYAN -> Color(0xFF00ACC1)
}
