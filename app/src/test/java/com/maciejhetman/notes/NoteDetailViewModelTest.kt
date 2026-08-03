package com.maciejhetman.notes

import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.fakes.FakeNoteRepository
import com.maciejhetman.notes.testutil.MainDispatcherRule
import com.maciejhetman.notes.ui.viewmodel.NoteDetailViewModel
import com.maciejhetman.notes.ui.viewmodel.SavedState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var repository: FakeNoteRepository

    @Before
    fun setUp() {
        repository = FakeNoteRepository()
    }

    @Test
    fun `empty new note is never inserted nor marked saved`() = runTest(testDispatcher) {
        val viewModel = NoteDetailViewModel(repository, noteId = null)

        // Editing only whitespace still schedules an autosave pass.
        viewModel.updateTitle("")
        viewModel.updateContent("   ")
        advanceTimeBy(1_500 + 1)
        advanceUntilIdle()

        assertTrue(repository.insertedNotes.isEmpty())
        assertTrue(repository.updatedNotes.isEmpty())
        assertNotEquals(SavedState.Saved, viewModel.uiState.value.savedState)

        // Explicit save of a still-blank new note must not write either.
        viewModel.saveNote()
        advanceUntilIdle()

        assertTrue(repository.insertedNotes.isEmpty())
        assertTrue(repository.updatedNotes.isEmpty())
        assertNotEquals(SavedState.Saved, viewModel.uiState.value.savedState)
    }

    @Test
    fun `editing a new note autosaves by inserting it and flips state to saved`() = runTest(testDispatcher) {
        val viewModel = NoteDetailViewModel(repository, noteId = null)
        assertEquals(SavedState.Idle, viewModel.uiState.value.savedState)

        viewModel.updateTitle("Shopping")
        viewModel.updateContent("- milk")
        assertEquals(SavedState.Unsaved, viewModel.uiState.value.savedState)

        // Autosave debounce is 1500 ms.
        advanceTimeBy(1_500 + 1)
        advanceUntilIdle()

        assertEquals(1, repository.insertedNotes.size)
        assertTrue(repository.updatedNotes.isEmpty())
        val inserted = repository.insertedNotes.single()
        assertEquals("Shopping", inserted.title)
        assertEquals("- milk", inserted.content)

        val state = viewModel.uiState.value
        assertEquals(SavedState.Saved, state.savedState)
        assertFalse(state.isNew)
        assertNotNull(state.id)
        assertEquals(inserted.id, state.id)
    }

    @Test
    fun `existing note is updated not inserted`() = runTest(testDispatcher) {
        val existing = Note(id = 7L, title = "Old title", content = "Old content", createdAt = 1_000L)
        repository = FakeNoteRepository(listOf(existing))
        val viewModel = NoteDetailViewModel(repository, noteId = 7L)
        advanceUntilIdle()

        // Loaded from the repository and considered saved.
        assertEquals("Old title", viewModel.uiState.value.title)
        assertEquals(SavedState.Saved, viewModel.uiState.value.savedState)

        viewModel.updateTitle("New title")
        advanceTimeBy(1_500 + 1)
        advanceUntilIdle()

        assertTrue(repository.insertedNotes.isEmpty())
        assertEquals(1, repository.updatedNotes.size)
        val updated = repository.updatedNotes.single()
        assertEquals(7L, updated.id)
        assertEquals("New title", updated.title)
        assertEquals("Old content", updated.content)
        assertEquals(SavedState.Saved, viewModel.uiState.value.savedState)
        assertFalse(viewModel.uiState.value.isNew)
    }

    @Test
    fun `saveNote no-ops when state is not unsaved`() = runTest(testDispatcher) {
        // Fresh new note — Idle, never edited.
        val newViewModel = NoteDetailViewModel(repository, noteId = null)
        newViewModel.saveNote()
        advanceUntilIdle()
        assertTrue(repository.insertedNotes.isEmpty())
        assertTrue(repository.updatedNotes.isEmpty())
        assertEquals(SavedState.Idle, newViewModel.uiState.value.savedState)

        // Existing note that was only opened — Saved, no pending edits.
        val existing = Note(id = 3L, title = "Keep", content = "As is", createdAt = 500L)
        repository = FakeNoteRepository(listOf(existing))
        val existingViewModel = NoteDetailViewModel(repository, noteId = 3L)
        advanceUntilIdle()
        assertEquals(SavedState.Saved, existingViewModel.uiState.value.savedState)

        existingViewModel.saveNote()
        advanceUntilIdle()
        assertTrue(repository.insertedNotes.isEmpty())
        assertTrue(repository.updatedNotes.isEmpty())
    }
}
