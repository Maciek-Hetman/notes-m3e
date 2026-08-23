package com.maciejhetman.notes

import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.fakes.FakeNoteRepository
import com.maciejhetman.notes.fakes.FakeFolderRepository
import com.maciejhetman.notes.testutil.MainDispatcherRule
import com.maciejhetman.notes.ui.viewmodel.DateRangeFilter
import com.maciejhetman.notes.ui.viewmodel.ListSection
import com.maciejhetman.notes.ui.viewmodel.NoteListViewModel
import com.maciejhetman.notes.ui.viewmodel.SortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private fun note(
        id: Long,
        title: String = "note$id",
        content: String = "",
        createdAt: Long = 0L,
        modifiedAt: Long = 0L,
        folderId: Long? = null
    ) = Note(
        id = id,
        folderId = folderId,
        title = title,
        content = content,
        createdAt = createdAt,
        modifiedAt = modifiedAt
    )

    @Test
    fun `sort comparators order notes as expected`() {
        val a = note(1, title = "apple", createdAt = 1, modifiedAt = 30)
        val b = note(2, title = "Banana", createdAt = 3, modifiedAt = 10)
        val c = note(3, title = "cherry", createdAt = 2, modifiedAt = 20)
        val notes = listOf(c, a, b)

        assertEquals(listOf(a, b, c), notes.sortedWith(SortOption.TITLE_ASC.comparator))
        assertEquals(listOf(c, b, a), notes.sortedWith(SortOption.TITLE_DESC.comparator))
        assertEquals(listOf(b, c, a), notes.sortedWith(SortOption.CREATED_NEWEST.comparator))
        assertEquals(listOf(a, c, b), notes.sortedWith(SortOption.CREATED_OLDEST.comparator))
        assertEquals(listOf(a, c, b), notes.sortedWith(SortOption.MODIFIED_NEWEST.comparator))
        assertEquals(listOf(b, c, a), notes.sortedWith(SortOption.MODIFIED_OLDEST.comparator))
    }

    @Test
    fun `changing sort option resorts the emitted notes`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(
            listOf(
                note(1, title = "bbb", modifiedAt = 20),
                note(2, title = "aaa", modifiedAt = 10)
            )
        )
        val viewModel = NoteListViewModel(repository, FakeFolderRepository())
        // notesUiState is stateIn(WhileSubscribed) — it only emits while collected.
        backgroundScope.launch { viewModel.notesUiState.collect { } }
        advanceUntilIdle()

        assertEquals(SortOption.MODIFIED_NEWEST, viewModel.notesUiState.value.sortOption)
        assertEquals(listOf(1L, 2L), viewModel.notesUiState.value.notes.map { it.id })

        viewModel.onSortOptionChange(SortOption.TITLE_ASC)
        advanceUntilIdle()

        assertEquals(SortOption.TITLE_ASC, viewModel.notesUiState.value.sortOption)
        assertEquals(listOf(2L, 1L), viewModel.notesUiState.value.notes.map { it.id })
    }

    @Test
    fun `date range filter keeps only notes created inside the range`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(
            listOf(
                note(1, createdAt = 100),
                note(2, createdAt = 200),
                note(3, createdAt = 300)
            )
        )
        val viewModel = NoteListViewModel(repository, FakeFolderRepository())
        backgroundScope.launch { viewModel.notesUiState.collect { } }
        advanceUntilIdle()
        assertEquals(3, viewModel.notesUiState.value.notes.size)
        assertNull(viewModel.notesUiState.value.dateRangeFilter)

        viewModel.onDateRangeFilterChange(DateRangeFilter(startInclusive = 150, endInclusive = 300))
        advanceUntilIdle()

        assertEquals(setOf(2L, 3L), viewModel.notesUiState.value.notes.map { it.id }.toSet())
        assertEquals(DateRangeFilter(150, 300), viewModel.notesUiState.value.dateRangeFilter)

        viewModel.onClearDateRangeFilter()
        advanceUntilIdle()

        assertEquals(3, viewModel.notesUiState.value.notes.size)
        assertNull(viewModel.notesUiState.value.dateRangeFilter)
    }

    @Test
    fun `search delegates to repository searchNotes`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(
            listOf(
                note(1, title = "buy milk"),
                note(2, title = "call mom")
            )
        )
        val viewModel = NoteListViewModel(repository, FakeFolderRepository())
        backgroundScope.launch { viewModel.notesUiState.collect { } }
        advanceUntilIdle()

        // Empty query streams all notes without touching searchNotes.
        assertNull(repository.lastSearchQuery)
        assertEquals(2, viewModel.notesUiState.value.notes.size)

        viewModel.onSearchQueryChange("milk")
        advanceUntilIdle()

        assertEquals("milk", repository.lastSearchQuery)
        assertEquals("milk", viewModel.searchQuery.value)
        assertEquals(listOf(1L), viewModel.notesUiState.value.notes.map { it.id })

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()
        assertEquals(2, viewModel.notesUiState.value.notes.size)
    }

    @Test
    fun `delete moves note to trash and undo restores it`() = runTest(testDispatcher) {
        val victim = note(5, title = "doomed")
        val repository = FakeNoteRepository(listOf(note(4, title = "safe"), victim))
        val viewModel = NoteListViewModel(repository, FakeFolderRepository())
        backgroundScope.launch { viewModel.notesUiState.collect { } }
        advanceUntilIdle()
        assertEquals(2, viewModel.notesUiState.value.notes.size)

        viewModel.deleteNote(victim)
        advanceUntilIdle()

        assertEquals(listOf(5L), repository.trashedNotes.map { it.id })
        assertEquals(listOf(4L), viewModel.notesUiState.value.notes.map { it.id })

        viewModel.restoreNote(victim)
        advanceUntilIdle()

        assertEquals(listOf(5L), repository.restoredNotes.map { it.id })
        assertEquals(setOf(4L, 5L), viewModel.notesUiState.value.notes.map { it.id }.toSet())
    }

    @Test
    fun `permanently delete removes the note entirely`() = runTest(testDispatcher) {
        val victim = note(5, title = "doomed")
        val repository = FakeNoteRepository(listOf(note(4, title = "safe"), victim))
        val viewModel = NoteListViewModel(repository, FakeFolderRepository())
        backgroundScope.launch { viewModel.notesUiState.collect { } }
        advanceUntilIdle()

        viewModel.permanentlyDeleteNote(victim)
        advanceUntilIdle()

        assertEquals(listOf(5L), repository.deletedNotes.map { it.id })
        assertEquals(listOf(4L), viewModel.notesUiState.value.notes.map { it.id })
    }

    @Test
    fun `to-do section shows only notes containing todo markers`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(
            listOf(
                note(1, title = "plain", content = "just text"),
                note(2, title = "shopping", content = "- [ ] milk"),
                note(3, title = "done", content = "* [X] walk the dog")
            )
        )
        val viewModel = NoteListViewModel(repository, FakeFolderRepository())
        backgroundScope.launch { viewModel.notesUiState.collect { } }
        advanceUntilIdle()
        assertEquals(3, viewModel.notesUiState.value.notes.size)

        viewModel.onSectionChange(ListSection.TODOS)
        advanceUntilIdle()

        assertEquals(ListSection.TODOS, viewModel.notesUiState.value.section)
        assertEquals(listOf(2L, 3L), viewModel.notesUiState.value.notes.map { it.id })

        viewModel.onSectionChange(ListSection.NOTES)
        advanceUntilIdle()

        assertEquals(3, viewModel.notesUiState.value.notes.size)
    }

    @Test
    fun `deleted section lists only trashed notes and search filters within it`() =
        runTest(testDispatcher) {
            val trashed = note(1, title = "old plan", content = "- [ ] nothing left")
            val active = note(2, title = "current")
            val repository = FakeNoteRepository(listOf(trashed, active))
            val viewModel = NoteListViewModel(repository, FakeFolderRepository())
            backgroundScope.launch { viewModel.notesUiState.collect { } }
            advanceUntilIdle()

            viewModel.deleteNote(trashed)
            viewModel.onSectionChange(ListSection.DELETED)
            advanceUntilIdle()

            assertEquals(ListSection.DELETED, viewModel.notesUiState.value.section)
            assertEquals(listOf(1L), viewModel.notesUiState.value.notes.map { it.id })

            // Search inside trash matches on title/content client-side.
            viewModel.onSearchQueryChange("plan")
            advanceUntilIdle()
            assertEquals(listOf(1L), viewModel.notesUiState.value.notes.map { it.id })

            viewModel.onSearchQueryChange("current")
            advanceUntilIdle()
            assertEquals(emptyList<Long>(), viewModel.notesUiState.value.notes.map { it.id })

            // Active notes never leak into the Deleted section.
            viewModel.onSectionChange(ListSection.NOTES)
            advanceUntilIdle()
            assertEquals(listOf(2L), viewModel.notesUiState.value.notes.map { it.id })
        }

    @Test
    fun `search inside a folder only returns matching notes from that folder`() =
        runTest(testDispatcher) {
            val repository = FakeNoteRepository(
                listOf(
                    note(1, title = "milk in folder", folderId = 10L),
                    note(2, title = "milk at root", folderId = null)
                )
            )
            val viewModel = NoteListViewModel(repository, FakeFolderRepository(), folderId = 10L)
            backgroundScope.launch { viewModel.notesUiState.collect { } }
            advanceUntilIdle()

            viewModel.onSearchQueryChange("milk")
            advanceUntilIdle()

            assertEquals("milk", repository.lastSearchQuery)
            assertTrue(repository.didSearchInFolder)
            assertEquals(10L, repository.lastSearchFolderId)
            assertEquals(listOf(1L), viewModel.notesUiState.value.notes.map { it.id })
        }

    @Test
    fun `to-do section at root includes notes that live in folders`() = runTest(testDispatcher) {
        val repository = FakeNoteRepository(
            listOf(
                note(1, title = "root todo", content = "- [ ] milk"),
                note(2, title = "nested todo", content = "* [ ] eggs", folderId = 4L),
                note(3, title = "plain nested", content = "hello", folderId = 4L)
            )
        )
        val viewModel = NoteListViewModel(repository, FakeFolderRepository())
        backgroundScope.launch { viewModel.notesUiState.collect { } }
        advanceUntilIdle()

        viewModel.onSectionChange(ListSection.TODOS)
        advanceUntilIdle()

        assertEquals(setOf(1L, 2L), viewModel.notesUiState.value.notes.map { it.id }.toSet())
    }
}
