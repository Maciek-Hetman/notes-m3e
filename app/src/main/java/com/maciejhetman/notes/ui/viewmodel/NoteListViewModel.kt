package com.maciejhetman.notes.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciejhetman.notes.data.Folder
import com.maciejhetman.notes.data.FolderRepository
import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.data.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

    private val _sortOption = MutableStateFlow(SortOption.MODIFIED_NEWEST)

    private val _dateRangeFilter = MutableStateFlow<DateRangeFilter?>(null)

    val notesUiState: StateFlow<NoteListUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getNotesStreamByFolderId(folderId)
            } else {
                repository.searchNotes(query)
            }
        },
        _searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) {
                folderRepository.getSubfoldersStream(folderId)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        },
        _sortOption,
        _dateRangeFilter
    ) { notes, folders, sortOption, dateFilter ->
        val filtered = dateFilter?.let { filter ->
            notes.filter { it.createdAt in filter.startInclusive..filter.endInclusive }
        } ?: notes
        NoteListUiState(
            isLoading = false,
            folders = folders,
            notes = filtered.sortedWith(sortOption.comparator),
            sortOption = sortOption,
            dateRangeFilter = dateFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteListUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
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
                repository.deleteNote(note)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete note", e)
            }
        }
    }

    fun undoDelete(note: Note) {
        viewModelScope.launch {
            try {
                repository.insertNote(note)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to undo delete", e)
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                folderRepository.insertFolder(Folder(name = name, parentFolderId = folderId))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create folder", e)
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            try {
                folderRepository.deleteFolder(folder)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete folder", e)
            }
        }
    }

    companion object {
        private const val TAG = "NoteListViewModel"
    }
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
    val dateRangeFilter: DateRangeFilter? = null
)
