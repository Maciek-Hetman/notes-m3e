package com.maciejhetman.notes.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciejhetman.notes.data.Folder
import com.maciejhetman.notes.data.FolderRepository
import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.data.NoteRepository
import com.maciejhetman.notes.ui.util.deleteInternalImagesReferencedBy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModel(
    private val repository: NoteRepository,
    private val folderRepository: FolderRepository,
    private val folderId: Long? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _section = MutableStateFlow(ListSection.NOTES)

    private val _sortOption = MutableStateFlow(SortOption.MODIFIED_NEWEST)

    private val _dateRangeFilter = MutableStateFlow<DateRangeFilter?>(null)

    /** Whether this list is scoped to a folder (drives back-arrow vs menu icon in the top bar). */
    val isInFolder: Boolean
        get() = folderId != null

    private data class SectionQuery(val section: ListSection, val query: String)

    private val sectionAndQuery = combine(_section, _searchQuery, ::SectionQuery)

    val notesUiState: StateFlow<NoteListUiState> = combine(
        sectionAndQuery.flatMapLatest { (section, query) ->
            when (section) {
                ListSection.DELETED -> repository.getDeletedNotesStream()
                ListSection.TODOS -> when {
                    query.isNotEmpty() && folderId != null ->
                        repository.searchNotesInFolder(query, folderId)
                    query.isNotEmpty() ->
                        repository.searchNotes(query)
                    folderId != null ->
                        repository.getNotesStreamByFolderId(folderId)
                    else ->
                        repository.getAllNotesStream()
                }
                ListSection.NOTES -> when {
                    query.isEmpty() -> repository.getNotesStreamByFolderId(folderId)
                    folderId != null -> repository.searchNotesInFolder(query, folderId)
                    else -> repository.searchNotes(query)
                }
            }
        },
        sectionAndQuery.flatMapLatest { (section, query) ->
            if (section == ListSection.NOTES && query.isEmpty()) {
                folderRepository.getSubfoldersStream(folderId)
            } else {
                flowOf(emptyList())
            }
        },
        _sortOption,
        _dateRangeFilter,
        sectionAndQuery
    ) { notes, folders, sortOption, dateFilter, (section, query) ->
        val dateFiltered = dateFilter?.let { filter ->
            notes.filter { it.createdAt in filter.startInclusive..filter.endInclusive }
        } ?: notes
        val visible = when (section) {
            ListSection.DELETED ->
                if (query.isEmpty()) dateFiltered
                else dateFiltered.filter { matchesQuery(it, query) }
            ListSection.TODOS -> dateFiltered.filter { it.content.contains(TODO_LINE_REGEX) }
            ListSection.NOTES -> dateFiltered
        }
        NoteListUiState(
            isLoading = false,
            folders = folders,
            notes = visible.sortedWith(sortOption.comparator),
            sortOption = sortOption,
            dateRangeFilter = dateFilter,
            section = section
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteListUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSectionChange(section: ListSection) {
        _section.value = section
    }

    fun onSortOptionChange(option: SortOption) {
        _sortOption.value = option
    }

    fun onDateRangeFilterChange(filter: DateRangeFilter?) {
        _dateRangeFilter.value = filter
    }

    fun onClearDateRangeFilter() {
        _dateRangeFilter.value = null
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                repository.moveToTrash(note)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to move note to trash", e)
            }
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            try {
                repository.restoreFromTrash(note)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore note", e)
            }
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            try {
                deleteInternalImagesReferencedBy(note.content)
                repository.deleteNote(note)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to permanently delete note", e)
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                folderRepository.insertFolder(Folder(name = name, parentFolderId = folderId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create folder", e)
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            try {
                val folderIds = collectFolderIds(folder.id)
                val trashed = repository.getDeletedNotesStream().first()
                    .filter { it.folderId in folderIds }
                val active = folderIds.flatMap { id ->
                    repository.getNotesStreamByFolderId(id).first()
                }
                (active + trashed).forEach { deleteInternalImagesReferencedBy(it.content) }
                folderRepository.deleteFolder(folder)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete folder", e)
            }
        }
    }

    private suspend fun collectFolderIds(rootId: Long): Set<Long> {
        val ids = mutableSetOf(rootId)
        val children = folderRepository.getSubfoldersStream(rootId).first()
        for (child in children) {
            ids += collectFolderIds(child.id)
        }
        return ids
    }

    companion object {
        private const val TAG = "NoteListViewModel"

        /** Markdown todo markers at line starts: `- [ ] `, `- [x] `, `* [X]`, etc. */
        val TODO_LINE_REGEX = Regex("(?m)^\\s*[-*]\\s*\\[[ xX]\\]")

        private fun matchesQuery(note: Note, query: String): Boolean =
            note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)
    }
}

/** Top-level list sections surfaced in the home-screen menu. */
enum class ListSection(val label: String) {
    NOTES("Notes"),
    TODOS("To-dos"),
    DELETED("Deleted")
}

enum class SortOption(val label: String, val comparator: Comparator<Note>) {
    TITLE_ASC("Title A-Z", compareBy { it.title.lowercase() }),
    TITLE_DESC("Title Z-A", compareByDescending { it.title.lowercase() }),
    CREATED_NEWEST("Created (newest)", compareByDescending { it.createdAt }),
    CREATED_OLDEST("Created (oldest)", compareBy { it.createdAt }),
    MODIFIED_NEWEST("Modified (newest)", compareByDescending { it.modifiedAt }),
    MODIFIED_OLDEST("Modified (oldest)", compareBy { it.modifiedAt })
}

data class DateRangeFilter(
    val startInclusive: Long,
    val endInclusive: Long
)

data class NoteListUiState(
    val isLoading: Boolean = true,
    val folders: List<Folder> = emptyList(),
    val notes: List<Note> = emptyList(),
    val sortOption: SortOption = SortOption.MODIFIED_NEWEST,
    val dateRangeFilter: DateRangeFilter? = null,
    val section: ListSection = ListSection.NOTES
)
