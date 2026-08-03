package com.maciejhetman.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.maciejhetman.notes.ui.components.buildInsertedValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownToolbarTest {

    private fun valueAt(text: String, cursor: Int) = TextFieldValue(text, TextRange(cursor))

    private fun selected(text: String, start: Int, end: Int) = TextFieldValue(text, TextRange(start, end))

    private fun assertInsertion(
        syntax: String,
        input: TextFieldValue,
        expectedText: String,
        expectedCursor: Int
    ) {
        val result = buildInsertedValue(syntax, input).newValue
        assertEquals(expectedText, result.text)
        assertEquals(TextRange(expectedCursor), result.selection)
    }

    @Test
    fun `headings without selection insert their prefix and place cursor after it`() {
        assertInsertion("h1", valueAt("", 0), "# ", 2)
        assertInsertion("h2", valueAt("", 0), "## ", 3)
        assertInsertion("h3", valueAt("", 0), "### ", 4)
        assertInsertion("h4", valueAt("", 0), "#### ", 5)
        // Inserted at the cursor position inside existing text.
        assertInsertion("h1", valueAt("abc", 0), "# abc", 2)
    }

    @Test
    fun `headings with selection prefix the selected text and cursor goes to the end`() {
        assertInsertion("h1", selected("hello world", 6, 11), "hello # world", 13)
        assertInsertion("h2", selected("hello world", 6, 11), "hello ## world", 14)
        assertInsertion("h3", selected("hello world", 6, 11), "hello ### world", 15)
        assertInsertion("h4", selected("hello world", 6, 11), "hello #### world", 16)
    }

    @Test
    fun `inline formats without selection insert empty markers with cursor inside`() {
        assertInsertion("bold", valueAt("", 0), "****", 2)
        assertInsertion("italic", valueAt("", 0), "**", 1)
        assertInsertion("underline", valueAt("", 0), "<u></u>", 3)
        assertInsertion("code", valueAt("", 0), "``", 1)
        // Mid-text insertion keeps surrounding text.
        assertInsertion("bold", valueAt("ab", 1), "a****b", 3)
    }

    @Test
    fun `inline formats with selection wrap the selected text`() {
        assertInsertion("bold", selected("hello world", 6, 11), "hello **world**", 15)
        assertInsertion("italic", selected("hello world", 6, 11), "hello *world*", 13)
        assertInsertion("underline", selected("hello world", 6, 11), "hello <u>world</u>", 18)
        assertInsertion("code", selected("hello world", 6, 11), "hello `world`", 13)
    }

    @Test
    fun `codeblock without selection inserts an empty fenced block`() {
        // At the start of the text or of a line: no leading newline.
        assertInsertion("codeblock", valueAt("", 0), "```\n\n```\n", 4)
        assertInsertion("codeblock", valueAt("abc\n", 4), "abc\n```\n\n```\n", 8)
        // Mid-line: separated from the current line first.
        assertInsertion("codeblock", valueAt("abc", 3), "abc\n```\n\n```\n", 8)
    }

    @Test
    fun `codeblock with selection wraps the selection in a fenced block`() {
        assertInsertion("codeblock", selected("abc", 0, 3), "```\nabc\n```\n", 12)
        assertInsertion("codeblock", selected("xabc", 1, 4), "x\n```\nabc\n```\n", 14)
    }

    @Test
    fun `list markers without selection respect line boundaries`() {
        // At start of text / start of a line.
        assertInsertion("ul", valueAt("", 0), "- ", 2)
        assertInsertion("ul", valueAt("abc\n", 4), "abc\n- ", 6)
        assertInsertion("ol", valueAt("", 0), "1. ", 3)
        assertInsertion("todo", valueAt("", 0), "- [ ] ", 6)
        // Mid-line: marker goes on a new line.
        assertInsertion("ul", valueAt("abc", 3), "abc\n- ", 6)
        assertInsertion("ol", valueAt("abc", 3), "abc\n1. ", 7)
        assertInsertion("todo", valueAt("abc", 3), "abc\n- [ ] ", 10)
    }

    @Test
    fun `list markers with selection replace the selected text`() {
        assertInsertion("ul", selected("abc", 0, 3), "- ", 2)
        assertInsertion("ol", selected("abc", 0, 3), "1. ", 3)
        assertInsertion("todo", selected("abc", 0, 3), "- [ ] ", 6)
    }

    @Test
    fun `quote respects line boundaries`() {
        assertInsertion("quote", valueAt("", 0), "> ", 2)
        assertInsertion("quote", valueAt("abc\n", 4), "abc\n> ", 6)
        assertInsertion("quote", valueAt("abc", 3), "abc\n> ", 6)
    }

    @Test
    fun `hr inserts a horizontal rule surrounded by newlines`() {
        assertInsertion("hr", valueAt("", 0), "\n---\n", 5)
        assertInsertion("hr", valueAt("abc", 1), "a\n---\nbc", 6)
    }

    @Test
    fun `unknown syntax is inserted literally with cursor after it`() {
        assertInsertion("foo", valueAt("", 0), "foo", 3)
        assertInsertion("foo", valueAt("ab", 1), "afoob", 4)
    }
}
