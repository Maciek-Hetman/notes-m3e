package com.maciejhetman.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Block types ────────────────────────────────────────────────────────────

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class UnorderedItem(val text: String) : MarkdownBlock()
    data class OrderedItem(val index: Int, val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class Image(val path: String, val alt: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
    object BlankLine : MarkdownBlock()
}

// ─── Parser ─────────────────────────────────────────────────────────────────

private val FENCE_REGEX = Regex("^```([^\n]*)$")
private val HORIZONTAL_RULE_LINE_REGEX = Regex("^(-{3,}|\\*{3,}|_{3,})$")
private val IMAGE_LINE_REGEX = Regex("^!\\[(.*?)\\]\\((.*?)\\)$")
private val ORDERED_LIST_LINE_REGEX = Regex("^\\d+\\.\\s+.*")
private val ORDERED_LIST_PREFIX_REGEX = Regex("^\\d+\\.\\s+")

private fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var orderedIndex = 1
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimEnd()
        val fenceMatch = FENCE_REGEX.find(trimmed)
        // Computed once up front (rather than matches() + find() inside the branch below) so an
        // image line is only ever matched against the regex a single time.
        val imageMatch = IMAGE_LINE_REGEX.matchEntire(trimmed)

        when {
            // Fenced code block — consume lines until the closing fence (or end of input)
            fenceMatch != null -> {
                val language = fenceMatch.groupValues[1].trim()
                val codeLines = mutableListOf<String>()
                var j = i + 1
                while (j < lines.size && lines[j].trimEnd() != "```") {
                    codeLines += lines[j]
                    j++
                }
                blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
                orderedIndex = 1
                i = j + 1
                continue
            }
            // Horizontal rule
            trimmed.matches(HORIZONTAL_RULE_LINE_REGEX) -> {
                blocks.add(MarkdownBlock.HorizontalRule)
                orderedIndex = 1
            }
            // ATX headings
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ")))
                orderedIndex = 1
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ")))
                orderedIndex = 1
            }
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ")))
                orderedIndex = 1
            }
            // Markdown image
            imageMatch != null -> {
                val alt = imageMatch.groupValues.getOrNull(1) ?: ""
                val path = imageMatch.groupValues.getOrNull(2) ?: ""
                blocks.add(MarkdownBlock.Image(path, alt))
                orderedIndex = 1
            }
            // Block quote
            trimmed.startsWith("> ") -> {
                blocks.add(MarkdownBlock.BlockQuote(trimmed.removePrefix("> ")))
                orderedIndex = 1
            }
            // Unordered list
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val text = trimmed.drop(2)
                blocks.add(MarkdownBlock.UnorderedItem(text))
                orderedIndex = 1
            }
            // Ordered list
            trimmed.matches(ORDERED_LIST_LINE_REGEX) -> {
                val text = trimmed.replaceFirst(ORDERED_LIST_PREFIX_REGEX, "")
                blocks.add(MarkdownBlock.OrderedItem(orderedIndex++, text))
            }
            // Blank line
            trimmed.isEmpty() -> {
                if (blocks.lastOrNull() !is MarkdownBlock.BlankLine) {
                    blocks.add(MarkdownBlock.BlankLine)
                }
                orderedIndex = 1
            }
            // Paragraph
            else -> {
                blocks.add(MarkdownBlock.Paragraph(trimmed))
                orderedIndex = 1
            }
        }
        i++
    }
    return blocks
}

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

// ─── Composable ─────────────────────────────────────────────────────────────

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    // Parsing markdown into blocks is a multi-pass line-by-line scan — skip redoing it on
    // recompositions that aren't caused by the text itself changing (e.g. a theme/color change).
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val quoteAccent = MaterialTheme.colorScheme.primaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    val codeHighlightColors = remember(primary, onSurface, tertiary, secondary) {
        CodeHighlightColors(
            keyword = primary,
            string = tertiary,
            number = secondary,
            comment = onSurface.copy(alpha = 0.45f)
        )
    }

    Column(modifier = modifier) {
        blocks.forEachIndexed { blockIndex, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = onSurface
                        )
                        2 -> MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                        else -> MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = onSurface
                        )
                    }
                    if (blockIndex > 0) Spacer(Modifier.height(8.dp))
                    Text(
                        text = remember(block.text, codeBackground) { parseInline(block.text, codeBackground) },
                        style = style
                    )
                    if (block.level == 1) {
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = primary.copy(alpha = 0.3f))
                    }
                    Spacer(Modifier.height(4.dp))
                }

                is MarkdownBlock.Image -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = rememberAsyncImagePainter(model = block.path),
                            contentDescription = block.alt.ifEmpty { null },
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(codeBackground, shape = RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        if (block.language.isNotEmpty()) {
                            Text(
                                text = block.language,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = onSurface.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = remember(block.code, block.language, codeHighlightColors) {
                                buildAnnotatedString {
                                    append(block.code)
                                    applySyntaxHighlighting(block.code, block.language, 0, codeHighlightColors)
                                }
                            },
                            style = baseStyle.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = onSurface
                            )
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = remember(block.text, codeBackground) { parseInline(block.text, codeBackground) },
                        style = baseStyle.copy(color = onSurface)
                    )
                }

                is MarkdownBlock.UnorderedItem -> {
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text(
                            text = "•",
                            style = baseStyle.copy(color = primary, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp)
                                .width(16.dp)
                        )
                        Text(
                            text = remember(block.text, codeBackground) { parseInline(block.text, codeBackground) },
                            style = baseStyle.copy(color = onSurface),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.OrderedItem -> {
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text(
                            text = "${block.index}.",
                            style = baseStyle.copy(color = primary, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp)
                                .width(24.dp)
                        )
                        Text(
                            text = remember(block.text, codeBackground) { parseInline(block.text, codeBackground) },
                            style = baseStyle.copy(color = onSurface),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.BlockQuote -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(24.dp)
                                .background(primary, shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = remember(block.text, codeBackground) { parseInline(block.text, codeBackground) },
                            style = baseStyle.copy(
                                color = onSurface.copy(alpha = 0.75f),
                                fontStyle = FontStyle.Italic
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .background(quoteAccent.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                MarkdownBlock.HorizontalRule -> {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(
                        color = primary.copy(alpha = 0.4f),
                        thickness = 1.5.dp
                    )
                    Spacer(Modifier.height(8.dp))
                }

                MarkdownBlock.BlankLine -> {
                    Spacer(Modifier.height(8.dp))
                }
            }
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
