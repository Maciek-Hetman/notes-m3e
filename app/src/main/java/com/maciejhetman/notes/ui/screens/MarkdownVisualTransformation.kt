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
class MarkdownVisualTransformation(
    private val primaryColor: Color,
    private val onSurfaceColor: Color,
    private val codeBackground: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        // Replace list hyphens/asterisks with a dot, preserving length
        val modifiedRaw = raw.replace(Regex("(?m)^(\\s*)[-*] (?!(\\[[ xX]\\]))"), "$1• ")

        val annotated = buildAnnotatedString {
            append(modifiedRaw)
            applyBlockStyles(modifiedRaw)
            applyInlineStyles(modifiedRaw)
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }

    // ── Block-level (per line) ─────────────────────────────────────────────

    private fun AnnotatedString.Builder.applyBlockStyles(text: String) {
        var offset = 0
        for (line in text.split('\n')) {
            val lineEnd = (offset + line.length).coerceAtMost(text.length)

            when {
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
                    // Make the "- [ ] " text completely transparent so we can overlay a stock Checkbox icon
                    // letterSpacing = 2.sp gives it a bit more width to ensure there's enough padding on the right before the text starts
                    addStyle(SpanStyle(color = Color.Transparent, letterSpacing = 2.sp), offset + spaceCount, (offset + spaceCount + 6).coerceAtMost(lineEnd))
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

    private fun AnnotatedString.Builder.applyInlineStyles(text: String) {
        // Track code span ranges so we don't style inside them
        val codeRanges = mutableListOf<IntRange>()

        // Inline code — first pass
        Regex("`([^`\n]+)`").findAll(text).forEach { match ->
            codeRanges += match.range
            addStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = codeBackground,
                    fontSize = 13.sp
                ),
                match.range.first, match.range.last + 1
            )
        }

        fun IntRange.isInsideCode() = codeRanges.any { it.first <= first && last <= it.last }

        // Bold + italic ***text***
        Regex("\\*{3}([^*\n]+?)\\*{3}").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    match.range.first, match.range.last + 1
                )
            }
        }

        // Bold **text**
        Regex("\\*\\*([^*\n]+?)\\*\\*").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }
        }

        // Italic *text*  (not preceded or followed by another *)
        Regex("(?<![*])\\*([^*\n]+?)\\*(?![*])").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            }
        }

        // Italic _text_
        Regex("_([^_\n]+?)_").findAll(text).forEach { match ->
            if (!match.range.isInsideCode()) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
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
    }
}
