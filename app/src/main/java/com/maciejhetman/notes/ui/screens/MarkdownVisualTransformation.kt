package com.maciejhetman.notes.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Obsidian-style Live Preview: applies Markdown visual styles inline without
 * modifying the underlying text. OffsetMapping is Identity since no characters
 * are added or removed — only SpanStyles are layered on top.
 */
import androidx.compose.ui.text.TextRange

class MarkdownVisualTransformation(
    private val primaryColor: Color,
    private val onSurfaceColor: Color,
    private val codeBackground: Color,
    private val selection: TextRange,
    private val imageAspectRatios: Map<String, Float>,
    // Width (in dp, expressed as a plain float) that images are actually rendered at.
    // Used so the reserved line-height matches the real displayed width/aspect-ratio
    // instead of an arbitrary assumption — otherwise the image ends up squashed/stretched.
    private val containerWidthDp: Float = 320f
) : VisualTransformation {

    companion object {
        // Fenced code block: ```language\n ...body... \n```
        // The language tag is optional; body is captured as a single (possibly multi-line) group.
        private val FENCED_CODE_REGEX = Regex("(?m)^```([^\n]*)\n([\\s\\S]*?)\n```[ \t]*$")
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val fencedMatches = FENCED_CODE_REGEX.findAll(raw).toList()
        fun isInsideFence(range: IntRange) = fencedMatches.any { it.range.first <= range.first && range.last <= it.range.last }

        // Replace list hyphens/asterisks with a dot, preserving length — but leave fenced
        // code bodies untouched (e.g. a shell comment starting with "- " shouldn't become a bullet).
        val listMarkerRegex = Regex("(?m)^(\\s*)[-*] (?!(\\[[ xX]\\]))")
        val modifiedRaw = listMarkerRegex.replace(raw) { match ->
            if (isInsideFence(match.range)) match.value else "${match.groupValues[1]}• "
        }

        val annotated = buildAnnotatedString {
            append(modifiedRaw)
            applyBlockStyles(modifiedRaw, fencedMatches)
            applyFencedCodeBlocks(fencedMatches)
            applyInlineStyles(modifiedRaw, fencedMatches)
        }
        return TransformedText(annotated, OffsetMapping.Identity)
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
            val contentStart = langEnd + 1 // skip the newline after the language tag
            val contentEnd = contentStart + content.length
            val closeFenceStart = contentEnd + 1 // skip the newline before the closing fence
            val closeFenceEnd = closeFenceStart + 3

            // Make the ``` fences disappear entirely, just like inline code backticks.
            addStyle(hiddenFenceStyle, openFenceStart, langStart)
            addStyle(hiddenFenceStyle, closeFenceStart, closeFenceEnd)

            // Dim the language tag into a small caption instead of hiding it — it's useful context.
            if (language.isNotEmpty()) {
                addStyle(
                    SpanStyle(
                        color = onSurfaceColor.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    langStart, langEnd
                )
            }

            // The code body itself: monospace font with a highlighted background, same treatment
            // as inline code but spanning every line of the block.
            if (contentEnd > contentStart) {
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        fontSize = 13.sp
                    ),
                    contentStart, contentEnd
                )
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
                    addStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), offset, lineEnd)
                    addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = 11.sp), offset, (offset + 5).coerceAtMost(lineEnd))
                }
                // H3 — must check before H2 and H1
                line.startsWith("### ") -> {
                    addStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), offset, lineEnd)
                    // Dim the ### marker
                    addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = 12.sp), offset, (offset + 4).coerceAtMost(lineEnd))
                }
                // H2 — must check before H1
                line.startsWith("## ") -> {
                    addStyle(SpanStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold), offset, lineEnd)
                    addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = 14.sp), offset, (offset + 3).coerceAtMost(lineEnd))
                }
                // H1
                line.startsWith("# ") -> {
                    addStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold), offset, lineEnd)
                    addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = 17.sp), offset, (offset + 2).coerceAtMost(lineEnd))
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
                    // Width is shrunk via textGeometricTransform (not fontSize) so the reserved gap before the
                    // item text stays small and predictable. fontSize is bumped slightly above body text so
                    // this line's natural height comfortably fits the overlaid checkbox icon.
                    addStyle(
                        SpanStyle(
                            color = Color.Transparent,
                            fontSize = 20.sp,
                            textGeometricTransform = androidx.compose.ui.text.style.TextGeometricTransform(scaleX = 0.55f)
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
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val spaceIdx = line.indexOf(' ')
                    if (spaceIdx > 0) {
                        addStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold), offset, (offset + spaceIdx + 1).coerceAtMost(lineEnd))
                    }
                }
                // Horizontal rule
                line.matches(Regex("^-{3,}$|^\\*{3,}$|^_{3,}$")) -> {
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
        val markerStyle = SpanStyle(color = onSurfaceColor.copy(alpha = 0.3f), fontSize = 11.sp)

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
        Regex("`([^`\n]+)`").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                codeRanges += match.range
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        fontSize = 13.sp
                    ),
                    match.range.first, match.range.last + 1
                )
                // Make the surrounding backticks disappear entirely.
                hideDelimiter(match.range.first, 1)
                hideDelimiter(match.range.last, 1)
            }
        }

        // Bold + italic ***text***
        Regex("\\*{3}([^*\n]+?)\\*{3}").findAll(text).forEach { match ->
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
        Regex("\\*\\*([^*\n]+?)\\*\\*").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                dimDelimiter(match.range.first, 2)
                dimDelimiter(match.range.last - 1, 2)
            }
        }

        // Italic *text*  (not preceded or followed by another *)
        Regex("(?<![*])\\*([^*\n]+?)\\*(?![*])").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
                dimDelimiter(match.range.first, 1)
                dimDelimiter(match.range.last, 1)
            }
        }

        // Italic _text_
        Regex("_([^_\n]+?)_").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
                dimDelimiter(match.range.first, 1)
                dimDelimiter(match.range.last, 1)
            }
        }

        // Underline <u>text</u>
        Regex("<u>(.*?)</u>").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), match.range.first, match.range.last + 1)
                // Dim the tags
                addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = 12.sp), match.range.first, match.range.first + 3)
                addStyle(SpanStyle(color = primaryColor.copy(alpha = 0.35f), fontSize = 12.sp), match.range.last - 3, match.range.last + 1)
            }
        }

        // Markdown Images ![alt](uri)
        Regex("!\\[.*?\\]\\((.*?)\\)").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                val isCursorInside = selection.start in match.range.first..(match.range.last + 1)
                if (isCursorInside) {
                    addStyle(
                        SpanStyle(
                            color = primaryColor.copy(alpha = 0.45f),
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp
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
