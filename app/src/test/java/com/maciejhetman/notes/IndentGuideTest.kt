package com.maciejhetman.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.data.IndentGuideColor
import com.maciejhetman.notes.data.LineNumberMode
import com.maciejhetman.notes.ui.screens.MarkdownVisualTransformation
import com.maciejhetman.notes.ui.screens.computeIndentGuides
import com.maciejhetman.notes.ui.screens.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndentGuideTest {

    private fun transformation(
        lineNumberMode: LineNumberMode = LineNumberMode.OFF,
        gutterWidth: Float = 0f
    ) = MarkdownVisualTransformation(
        primaryColor = Color.White,
        onSurfaceColor = Color.White,
        codeBackground = Color.Gray,
        selection = TextRange(0),
        imageAspectRatios = emptyMap(),
        lineNumberMode = lineNumberMode,
        gutterWidth = gutterWidth.sp
    )

    private fun AnnotatedString.textIndentAt(position: Int): Float =
        paragraphStyles.firstOrNull { position in it.start until it.end }
            ?.item?.textIndent?.firstLine?.value ?: 0f

    @Test
    fun `indent level rounds leading whitespace up to the nearest level`() {
        val guides = computeIndentGuides("a\n  b\n    c\n        d")
        assertEquals(listOf(0, 1, 1, 2), guides.map { it.level })
    }

    @Test
    fun `tab counts as one level`() {
        val guides = computeIndentGuides("a\n\tb\n\t\tc")
        assertEquals(listOf(0, 1, 2), guides.map { it.level })
    }

    @Test
    fun `level is capped at max indent levels`() {
        val guides = computeIndentGuides("a\n" + " ".repeat(64) + "b")
        assertEquals(listOf(0, 8), guides.map { it.level })
    }

    @Test
    fun `lines inside fenced code blocks are flagged`() {
        val text = "```kotlin\nfun main() {\n    val x = 1\n}\n```"
        val guides = computeIndentGuides(text)
        val indentedCodeLine = guides.first { it.level == 1 }
        assertTrue(indentedCodeLine.insideFence)
        val indentedText = guides.firstOrNull { it.level == 1 && !it.insideFence }
        assertEquals(null, indentedText)
    }

    @Test
    fun `plain text lines outside fences are not inside a fence`() {
        val text = "  item\n```\n  code\n```"
        val guides = computeIndentGuides(text)
        assertEquals(false, guides[0].insideFence)
        assertEquals(true, guides[2].insideFence)
    }

    @Test
    fun `text indentation does not add paragraph styles`() {
        val transformed = transformation()
            .filter(AnnotatedString("  indented text"))
            .text
        assertNull(transformed.paragraphStyles.firstOrNull())
    }

    @Test
    fun `code block padding is applied when line numbers are off`() {
        val text = "```kotlin\nfun main() {\n}\n```"
        val contentStart = "```kotlin\n".length
        val transformed = transformation()
            .filter(AnnotatedString(text))
            .text
        // inner padding = 10sp
        assertEquals(10f, transformed.textIndentAt(contentStart))
    }

    @Test
    fun `code block padding includes gutter when line numbers are on`() {
        val text = "```kotlin\nfun main() {\n}\n```"
        val contentStart = "```kotlin\n".length
        val transformed = transformation(
            lineNumberMode = LineNumberMode.CODE_BLOCKS_ONLY,
            gutterWidth = 36f
        ).filter(AnnotatedString(text)).text
        // gutter + inner padding = 36 + 10 = 46sp
        assertEquals(46f, transformed.textIndentAt(contentStart))
    }

    @Test
    fun `all lines mode applies gutter plus inner padding everywhere`() {
        val transformed = transformation(
            lineNumberMode = LineNumberMode.ALL_LINES,
            gutterWidth = 36f
        ).filter(AnnotatedString("line one\nline two")).text
        assertEquals(46f, transformed.textIndentAt(0))
    }

    @Test
    fun `auto color follows onSurfaceVariant`() {
        val base = Color(0xFF123456)
        assertEquals(base.copy(alpha = 0.55f), IndentGuideColor.AUTO.resolve(base))
    }

    @Test
    fun `preset colors resolve to fixed values`() {
        assertEquals(Color(0xFF4A90D9), IndentGuideColor.BLUE.resolve(Color.White))
        assertEquals(Color(0xFF00ACC1), IndentGuideColor.CYAN.resolve(Color.Black))
    }
}
