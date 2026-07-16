package com.maciejhetman.notes.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    object HorizontalRule : MarkdownBlock()
    object BlankLine : MarkdownBlock()
}

// ─── Parser ─────────────────────────────────────────────────────────────────

private fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var orderedIndex = 1

    for (line in lines) {
        val trimmed = line.trimEnd()

        when {
            // Horizontal rule
            trimmed.matches(Regex("^(-{3,}|\\*{3,}|_{3,})$")) -> {
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
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                val text = trimmed.replaceFirst(Regex("^\\d+\\.\\s+"), "")
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
            i + 2 < text.length && text.substring(i, i + 3) == "***" -> {
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
            i + 1 < text.length && text.substring(i, i + 2) == "**" -> {
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
    val blocks = parseMarkdown(markdown)
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val quoteAccent = MaterialTheme.colorScheme.primaryContainer

    Column(modifier = modifier) {
        for ((blockIndex, block) in blocks.withIndex()) {
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
                        text = parseInline(block.text, codeBackground),
                        style = style
                    )
                    if (block.level == 1) {
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = primary.copy(alpha = 0.3f))
                    }
                    Spacer(Modifier.height(4.dp))
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInline(block.text, codeBackground),
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
                            text = parseInline(block.text, codeBackground),
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
                            text = parseInline(block.text, codeBackground),
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
                            text = parseInline(block.text, codeBackground),
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

/**
 * Strips common markdown syntax from a string to produce a plain-text preview snippet.
 */
fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("\\*(.*?)\\*"), "$1")
        .replace(Regex("`(.*?)`"), "$1")
        .replace(Regex("^>\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^-{3,}$", RegexOption.MULTILINE), "")
        .trim()
}
