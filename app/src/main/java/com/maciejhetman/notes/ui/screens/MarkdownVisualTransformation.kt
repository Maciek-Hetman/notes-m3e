package com.maciejhetman.notes.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.data.LineNumberMode

/**
 * Obsidian-style Live Preview: applies Markdown visual styles inline without
 * modifying the underlying text. OffsetMapping is Identity since no characters
 * are added or removed — only SpanStyles are layered on top.
 */
import androidx.compose.ui.text.TextRange

// Fenced code block: ```language\n ...body... \n```
// The language tag is optional; body is captured as a single (possibly multi-line) group.
// File-level so it can be shared with computeNumberedLines() below.
internal val FENCED_CODE_REGEX = Regex("(?m)^```([a-zA-Z0-9_+-]*)\\r?\\n((?:(?!^```)[\\s\\S])*?\\r?\\n)?```[ \\t]*(?:\\r?\\n|$)")

// All patterns below run once per keystroke (VisualTransformation.filter runs on every
// recomposition of the editing text field), so they're compiled once here rather than
// re-compiled from a string literal on every call.
private val LIST_MARKER_REGEX = Regex("(?m)^(\\s*)[-*] (?!(\\[[ xX]\\]))")
private val ORDERED_LIST_LINE_REGEX = Regex("^\\d+\\.\\s.*")
private val HORIZONTAL_RULE_LINE_REGEX = Regex("^-{3,}$|^\\*{3,}$|^_{3,}$")
private val INLINE_CODE_REGEX = Regex("`([^`\n]+)`")
private val BOLD_ITALIC_REGEX = Regex("\\*{3}([^*\n]+?)\\*{3}")
private val BOLD_REGEX = Regex("\\*\\*([^*\n]+?)\\*\\*")
private val ITALIC_STAR_REGEX = Regex("(?<![*])\\*([^*\n]+?)\\*(?![*])")
private val ITALIC_UNDERSCORE_REGEX = Regex("_([^_\n]+?)_")
private val UNDERLINE_REGEX = Regex("<u>(.*?)</u>")
private val IMAGE_REGEX = Regex("!\\[.*?\\]\\((.*?)\\)")

/** A single logical line eligible for a rendered line-number gutter entry. */
data class NumberedLine(val startOffset: Int, val endOffsetExclusive: Int, val number: Int)

/**
 * Computes which lines of [text] should show a line number under [mode], and what number each
 * gets. Used both to reserve gutter indentation (MarkdownVisualTransformation) and to actually
 * draw the numbers as an overlay (NoteDetailScreen), so the two stay perfectly in sync.
 */
fun computeNumberedLines(text: String, mode: LineNumberMode): List<NumberedLine> {
    return when (mode) {
        LineNumberMode.OFF -> emptyList()
        LineNumberMode.ALL_LINES -> {
            val result = mutableListOf<NumberedLine>()
            var offset = 0
            var lineNumber = 0
            for (line in text.split('\n')) {
                lineNumber++
                val lineEnd = (offset + line.length).coerceAtMost(text.length)
                result += NumberedLine(offset, lineEnd, lineNumber)
                offset += line.length + 1
            }
            result
        }
        LineNumberMode.CODE_BLOCKS_ONLY -> {
            val result = mutableListOf<NumberedLine>()
            FENCED_CODE_REGEX.findAll(text).forEach { match ->
                val language = match.groupValues[1]
                val content = match.groupValues[2]
                val contentStart = match.range.first + 3 + language.length + 1
                var lineOffset = contentStart
                val lines = content.split('\n')
                val effectiveLines = if (lines.size > 1 && lines.last().isEmpty()) lines.dropLast(1) else lines
                effectiveLines.forEachIndexed { index, line ->
                    val lineEnd = (lineOffset + line.length).coerceAtMost(text.length)
                    result += NumberedLine(lineOffset, lineEnd, index + 1)
                    lineOffset += line.length + 1
                }
            }
            result
        }
    }
}

class MarkdownVisualTransformation(
    private val primaryColor: Color,
    private val onSurfaceColor: Color,
    private val codeBackground: Color,
    private val selection: TextRange,
    private val imageAspectRatios: Map<String, Float>,
    private val containerWidthDp: Float = 320f,
    keywordColor: Color = primaryColor,
    stringColor: Color = primaryColor,
    numberColor: Color = primaryColor,
    customHighlightColors: CodeHighlightColors? = null,
    private val fontFamily: FontFamily = FontFamily.Default,
    private val fontScale: Float = 1f,
    private val lineNumberMode: LineNumberMode = LineNumberMode.OFF,
    private val gutterWidth: TextUnit = 0.sp
) : VisualTransformation {

    private val codeHighlightColors = customHighlightColors ?: CodeHighlightColors(
        keyword = keywordColor,
        string = stringColor,
        number = numberColor,
        comment = onSurfaceColor.copy(alpha = 0.45f)
    )

    private val effectiveCodeBg = codeHighlightColors.background ?: codeBackground

    private fun scaledSp(base: Float) = (base * fontScale).sp

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val fencedMatches = FENCED_CODE_REGEX.findAll(raw).toList()
        fun isInsideFence(range: IntRange) = fencedMatches.any { it.range.first <= range.first && range.last <= it.range.last }

        val modifiedRaw = LIST_MARKER_REGEX.replace(raw) { match ->
            if (isInsideFence(match.range)) match.value else "${match.groupValues[1]}• "
        }

        val annotated = buildAnnotatedString {
            append(modifiedRaw)
            applyBlockStyles(modifiedRaw, fencedMatches)
            applyFencedCodeBlocks(fencedMatches)
            applyInlineStyles(modifiedRaw, fencedMatches)
            applyLineNumberIndent(modifiedRaw)
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }

    private fun AnnotatedString.Builder.applyLineNumberIndent(text: String) {
        if (text.isEmpty()) return
        val innerPaddingSp = scaledSp(10f)
        val gutterWithInnerIndent = (gutterWidth.value + innerPaddingSp.value).sp
        when (lineNumberMode) {
            LineNumberMode.ALL_LINES -> {
                addStyle(
                    ParagraphStyle(textIndent = TextIndent(gutterWithInnerIndent, gutterWithInnerIndent)),
                    0, text.length
                )
            }
            LineNumberMode.CODE_BLOCKS_ONLY -> {
                FENCED_CODE_REGEX.findAll(text).forEach { match ->
                    val language = match.groupValues[1]
                    val content = match.groupValues[2]
                    val contentStart = match.range.first + 3 + language.length + 1
                    val contentEnd = (contentStart + content.length).coerceAtMost(text.length)
                    if (contentEnd > contentStart) {
                        addStyle(
                            ParagraphStyle(textIndent = TextIndent(gutterWithInnerIndent, gutterWithInnerIndent)),
                            contentStart, contentEnd
                        )
                    }
                }
            }
            LineNumberMode.OFF -> {
                FENCED_CODE_REGEX.findAll(text).forEach { match ->
                    val language = match.groupValues[1]
                    val content = match.groupValues[2]
                    val contentStart = match.range.first + 3 + language.length + 1
                    val contentEnd = (contentStart + content.length).coerceAtMost(text.length)
                    if (contentEnd > contentStart) {
                        addStyle(
                            ParagraphStyle(textIndent = TextIndent(innerPaddingSp, innerPaddingSp)),
                            contentStart, contentEnd
                        )
                    }
                }
            }
        }
    }

    // ── Fenced code blocks (```lang ... ```) ────────────────────────────────

    private fun AnnotatedString.Builder.applyFencedCodeBlocks(fencedMatches: List<MatchResult>) {
        val hiddenFenceStyle = SpanStyle(color = Color.Transparent, fontSize = 0.sp)

        for (match in fencedMatches) {
            val language = match.groupValues[1]
            val content = match.groupValues[2]

            val openFenceStart = match.range.first
            val langStart = openFenceStart + 3
            val langEnd = langStart + language.length
            val contentStart = langEnd + 1 // skip newline after opening language tag

            val codeContentEnd = (contentStart + content.length).coerceAtMost(length)

            // Hide opening ```lang\n line
            addStyle(hiddenFenceStyle, openFenceStart, contentStart.coerceAtMost(length))

            // Hide ONLY the closing ``` backticks line
            val matchText = match.value
            val closingIndexInMatch = matchText.lastIndexOf("```")
            if (closingIndexInMatch != -1) {
                val absoluteCloseStart = match.range.first + closingIndexInMatch
                addStyle(hiddenFenceStyle, absoluteCloseStart, (absoluteCloseStart + 3).coerceAtMost(length))
            }

            if (codeContentEnd > contentStart) {
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = codeHighlightColors.textColor ?: Color.Unspecified,
                        fontSize = scaledSp(13f)
                    ),
                    contentStart, codeContentEnd
                )
                applySyntaxHighlighting(content, language, contentStart, codeHighlightColors)
            }
        }
    }



    // ── Block-level (per line) ─────────────────────────────────────────────

    private fun AnnotatedString.Builder.applyBlockStyles(text: String, fencedMatches: List<MatchResult>) {
        fun isInsideFence(pos: Int) = fencedMatches.any { pos in it.range }

        var offset = 0
        for (line in text.split('\n')) {
            val lineEnd = (offset + line.length).coerceAtMost(text.length)

            if (!isInsideFence(offset)) when {
                // H4 — must check before H3, H2, H1
                line.startsWith("#### ") -> {
                    addStyle(SpanStyle(fontSize = scaledSp(15f), fontWeight = FontWeight.SemiBold), offset, lineEnd)
                    addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = scaledSp(11f)), offset, (offset + 5).coerceAtMost(lineEnd))
                }
                // H3 — must check before H2 and H1
                line.startsWith("### ") -> {
                    addStyle(SpanStyle(fontSize = scaledSp(17f), fontWeight = FontWeight.SemiBold), offset, lineEnd)
                    // Dim the ### marker
                    addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = scaledSp(12f)), offset, (offset + 4).coerceAtMost(lineEnd))
                }
                // H2 — must check before H1
                line.startsWith("## ") -> {
                    addStyle(SpanStyle(fontSize = scaledSp(21f), fontWeight = FontWeight.Bold), offset, lineEnd)
                    addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = scaledSp(14f)), offset, (offset + 3).coerceAtMost(lineEnd))
                }
                // H1
                line.startsWith("# ") -> {
                    addStyle(SpanStyle(fontSize = scaledSp(26f), fontWeight = FontWeight.ExtraBold), offset, lineEnd)
                    addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = scaledSp(17f)), offset, (offset + 2).coerceAtMost(lineEnd))
                }
                // Blockquote
                line.startsWith("> ") -> {
                    addStyle(SpanStyle(color = onSurfaceColor.copy(alpha = 0.55f), fontStyle = FontStyle.Italic), offset, lineEnd)
                    // Make the > marker vibrant and non-italic
                    addStyle(SpanStyle(color = primaryColor, fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold), offset, (offset + 2).coerceAtMost(lineEnd))
                }
                // Todo list
                line.trimStart().let { it.startsWith("- [ ] ") || it.startsWith("- [x] ") || it.startsWith("- [X] ") } -> {
                    val spaceCount = line.length - line.trimStart().length
                    // Make the "- [ ] " text completely transparent so we can overlay a stock Checkbox icon.
                    // Width is controlled via textGeometricTransform (not fontSize) so the reserved gap
                    // before the item text stays predictable. scaleX is deliberately a bit wider than the
                    // checkbox icon itself (see NoteDetailScreen's checkboxEndPaddingPx) so there's always
                    // visible breathing room between the checkbox and the item text that follows it.
                    // fontSize is bumped slightly above body text so this line's natural height comfortably
                    // fits the overlaid checkbox icon.
                    addStyle(
                        SpanStyle(
                            color = Color.Transparent,
                            fontSize = scaledSp(20f),
                            textGeometricTransform = androidx.compose.ui.text.style.TextGeometricTransform(scaleX = 0.8f)
                        ),
                        offset + spaceCount, (offset + spaceCount + 6).coerceAtMost(lineEnd)
                    )
                }
                // Unordered list bullet
                line.trimStart().startsWith("• ") -> {
                    val spaceCount = line.length - line.trimStart().length
                    addStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.ExtraBold), offset + spaceCount, (offset + spaceCount + 2).coerceAtMost(lineEnd))
                }
                // Ordered list number
                line.matches(ORDERED_LIST_LINE_REGEX) -> {
                    val spaceIdx = line.indexOf(' ')
                    if (spaceIdx > 0) {
                        addStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold), offset, (offset + spaceIdx + 1).coerceAtMost(lineEnd))
                    }
                }
                // Horizontal rule
                line.matches(HORIZONTAL_RULE_LINE_REGEX) -> {
                    addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.25f)), offset, lineEnd)
                }
            }
            offset += line.length + 1 // +1 for the \n
        }
    }

    // ── Inline styles ──────────────────────────────────────────────────────

    private fun AnnotatedString.Builder.applyInlineStyles(text: String, fencedMatches: List<MatchResult>) {
        // Track code span ranges (fenced blocks + single-line `code`) so nothing else styles inside them.
        val codeRanges = mutableListOf<IntRange>()
        fencedMatches.forEach { codeRanges += it.range }

        // Style applied to emphasis delimiters (*, **, ***, _) so they shrink and
        // blend into the background instead of competing with the actual content.
        val markerStyle = SpanStyle(color = onSurfaceColor.copy(alpha = 0.3f), fontSize = scaledSp(11f))

        // A delimiter is fully hidden (zero advance width) — used for code span backticks,
        // which should disappear entirely rather than just blend in.
        val hiddenDelimiterStyle = SpanStyle(color = Color.Transparent, fontSize = 0.sp)

        fun hideDelimiter(start: Int, length: Int) {
            addStyle(hiddenDelimiterStyle, start, start + length)
        }

        fun dimDelimiter(start: Int, length: Int) {
            addStyle(markerStyle, start, start + length)
        }

        fun IntRange.isInsideCode() = codeRanges.any { it.first <= first && last <= it.last }

        // Inline code — first pass (skip anything already inside a fenced block)
        INLINE_CODE_REGEX.findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                codeRanges += match.range
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        fontSize = scaledSp(13f)
                    ),
                    match.range.first, match.range.last + 1
                )
                // Make the surrounding backticks disappear entirely.
                hideDelimiter(match.range.first, 1)
                hideDelimiter(match.range.last, 1)
            }
        }

        // Bold + italic ***text***
        BOLD_ITALIC_REGEX.findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    match.range.first, match.range.last + 1
                )
                dimDelimiter(match.range.first, 3)
                dimDelimiter(match.range.last - 2, 3)
            }
        }

        // Bold **text**
        BOLD_REGEX.findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                dimDelimiter(match.range.first, 2)
                dimDelimiter(match.range.last - 1, 2)
            }
        }

        // Italic *text*  (not preceded or followed by another *)
        ITALIC_STAR_REGEX.findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
                dimDelimiter(match.range.first, 1)
                dimDelimiter(match.range.last, 1)
            }
        }

        // Italic _text_
        ITALIC_UNDERSCORE_REGEX.findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
                dimDelimiter(match.range.first, 1)
                dimDelimiter(match.range.last, 1)
            }
        }

        // Underline <u>text</u>
        UNDERLINE_REGEX.findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), match.range.first, match.range.last + 1)
                // Dim the tags
                addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = scaledSp(12f)), match.range.first, match.range.first + 3)
                addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = scaledSp(12f)), match.range.last - 3, match.range.last + 1)
            }
        }

        // Markdown Images ![alt](uri)
        IMAGE_REGEX.findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                val isCursorInside = selection.start in match.range.first..(match.range.last + 1)
                if (isCursorInside) {
                    addStyle(
                        SpanStyle(
                            color = primaryColor.copy(alpha = 0.45f),
                            fontStyle = FontStyle.Italic,
                            fontSize = scaledSp(13f)
                        ),
                        match.range.first, match.range.last + 1
                    )
                } else {
                    val path = match.groupValues.getOrNull(1) ?: ""
                    val ratio = imageAspectRatios[path]
                    val effectiveWidth = if (containerWidthDp > 0) containerWidthDp else 320f
                    val heightSp = if (ratio != null && ratio > 0) {
                        (effectiveWidth / ratio).coerceIn(80f, 900f).sp
                    } else {
                        200.sp
                    }

                    // First character: transparent, sized to image height — reserves the line height.
                    addStyle(
                        SpanStyle(
                            color = Color.Transparent,
                            fontSize = heightSp
                        ),
                        match.range.first, match.range.first + 1
                    )
                    // Remaining characters: transparent, 0.sp — they have zero advance width so
                    // they never wrap to new lines and contribute no extra height.
                    if (match.range.last > match.range.first) {
                        addStyle(
                            SpanStyle(
                                color = Color.Transparent,
                                fontSize = 0.sp
                            ),
                            match.range.first + 1, match.range.last + 1
                        )
                    }
                }
            }
        }
    }
}
