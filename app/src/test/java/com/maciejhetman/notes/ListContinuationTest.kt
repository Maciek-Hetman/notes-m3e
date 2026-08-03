package com.maciejhetman.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.maciejhetman.notes.ui.util.applyListContinuation
import org.junit.Assert.assertEquals
import org.junit.Test

class ListContinuationTest {

    private fun valueAt(text: String, cursor: Int) = TextFieldValue(text, TextRange(cursor))

    private fun assertContinuation(
        old: TextFieldValue,
        new: TextFieldValue,
        expectedText: String,
        expectedCursor: Int
    ) {
        val result = applyListContinuation(old, new)
        assertEquals(expectedText, result.text)
        assertEquals(TextRange(expectedCursor), result.selection)
    }

    @Test
    fun `enter on a bullet item continues with a new bullet`() {
        assertContinuation(
            valueAt("- milk", 6), valueAt("- milk\n", 7),
            "- milk\n- ", 9
        )
        assertContinuation(
            valueAt("* tea", 5), valueAt("* tea\n", 6),
            "* tea\n* ", 8
        )
        // Indentation is preserved.
        assertContinuation(
            valueAt("  - sub", 7), valueAt("  - sub\n", 8),
            "  - sub\n  - ", 12
        )
    }

    @Test
    fun `enter on an ordered item increments the number`() {
        assertContinuation(
            valueAt("1. first", 8), valueAt("1. first\n", 9),
            "1. first\n2. ", 12
        )
        assertContinuation(
            valueAt("  3. item", 9), valueAt("  3. item\n", 10),
            "  3. item\n  4. ", 15
        )
        assertContinuation(
            valueAt("19. item", 8), valueAt("19. item\n", 9),
            "19. item\n20. ", 13
        )
    }

    @Test
    fun `enter on a todo item continues with an unchecked todo`() {
        assertContinuation(
            valueAt("- [ ] task", 10), valueAt("- [ ] task\n", 11),
            "- [ ] task\n- [ ] ", 17
        )
    }

    @Test
    fun `enter on a checked todo resets the next item to unchecked`() {
        assertContinuation(
            valueAt("- [x] done", 10), valueAt("- [x] done\n", 11),
            "- [x] done\n- [ ] ", 17
        )
        assertContinuation(
            valueAt("- [X] done", 10), valueAt("- [X] done\n", 11),
            "- [X] done\n- [ ] ", 17
        )
    }

    @Test
    fun `enter on an empty list item removes the marker instead`() {
        assertContinuation(
            valueAt("- ", 2), valueAt("- \n", 3),
            "\n", 1
        )
        assertContinuation(
            valueAt("1. ", 3), valueAt("1. \n", 4),
            "\n", 1
        )
        assertContinuation(
            valueAt("- [ ] ", 6), valueAt("- [ ] \n", 7),
            "\n", 1
        )
    }

    @Test
    fun `enter on an empty marker in the middle of text removes just that marker`() {
        assertContinuation(
            valueAt("- ab", 4), valueAt("- \nab", 3),
            "\nab", 1
        )
    }

    @Test
    fun `enter on a plain line does not continue anything`() {
        val old = valueAt("hello", 5)
        val new = valueAt("hello\n", 6)
        assertEquals(new, applyListContinuation(old, new))
    }

    @Test
    fun `multi-line paste does not misfire`() {
        // Two newlines appear at once — a paste, not an Enter keystroke.
        val pasted = valueAt("- a\n- b\n- c", 11)
        assertEquals(pasted, applyListContinuation(valueAt("- a", 3), pasted))
    }

    @Test
    fun `newline typed away from the cursor does not misfire`() {
        // A newline was added but the cursor is not right after it (autocorrect bundled edit).
        val old = valueAt("ab", 2)
        val new = valueAt("a\nb", 3)
        assertEquals(new, applyListContinuation(old, new))
    }
}
