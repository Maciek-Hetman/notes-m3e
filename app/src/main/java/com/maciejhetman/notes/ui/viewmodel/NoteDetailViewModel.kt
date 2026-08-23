package com.maciejhetman.notes.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.data.NoteRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

class NoteDetailViewModel(
    private val repository: NoteRepository,
    private val noteId: Long?,
    private val folderId: Long? = null,
    initialContent: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NoteDetailUiState(
            id = noteId,
            folderId = folderId,
            content = initialContent.orEmpty(),
            isNew = noteId == null,
            savedState = if (!initialContent.isNullOrBlank() && noteId == null) {
                SavedState.Unsaved
            } else {
                SavedState.Idle
            },
            initialContent = initialContent
        )
    )
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private val saveMutex = Mutex()

    init {
        if (noteId != null) {
            viewModelScope.launch {
                val note = repository.getNoteStream(noteId).filterNotNull().first()
                _uiState.update { current ->
                    // Typing that landed before Room answered must not be overwritten.
                    if (current.savedState == SavedState.Unsaved) {
                        current.copy(
                            id = note.id,
                            folderId = note.folderId,
                            createdAt = note.createdAt,
                            deletedAt = note.deletedAt,
                            isNew = false
                        )
                    } else {
                        current.copy(
                            id = note.id,
                            folderId = note.folderId,
                            title = note.title,
                            content = note.content,
                            createdAt = note.createdAt,
                            modifiedAt = note.modifiedAt,
                            deletedAt = note.deletedAt,
                            isNew = false,
                            savedState = SavedState.Saved
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, savedState = SavedState.Unsaved) }
        scheduleAutoSave()
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content, savedState = SavedState.Unsaved) }
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500.milliseconds)
            saveAndPublish()
        }
    }

    fun saveNote() {
        autoSaveJob?.cancel()
        // Nothing pending — the note was only opened/viewed, not edited. Skip the write
        // entirely so simply navigating into a note never bumps its modified timestamp.
        if (_uiState.value.savedState != SavedState.Unsaved) return
        viewModelScope.launch {
            saveAndPublish()
        }
    }

    private suspend fun saveAndPublish() {
        val snapshot = _uiState.value
        if (snapshot.savedState != SavedState.Unsaved) return
        if (snapshot.isNew && snapshot.title.isBlank() && snapshot.content.isBlank()) return
        _uiState.update { if (it.savedState == SavedState.Unsaved) it.copy(savedState = SavedState.Saving) else it }
        val modifiedAt = performSave(snapshot)
        if (modifiedAt != null) {
            _uiState.update { current ->
                if (current.title == snapshot.title && current.content == snapshot.content) {
                    current.copy(savedState = SavedState.Saved, modifiedAt = modifiedAt)
                } else {
                    current.copy(modifiedAt = modifiedAt)
                }
            }
        } else if (_uiState.value.savedState == SavedState.Saving) {
            _uiState.update { it.copy(savedState = SavedState.Error) }
        }
    }

    private suspend fun performSave(state: NoteDetailUiState): Long? = saveMutex.withLock {
        // Don't save truly empty new notes
        if (state.isNew && state.title.isBlank() && state.content.isBlank()) return null

        val modifiedAt = System.currentTimeMillis()
        val note = Note(
            id = state.id ?: 0,
            folderId = state.folderId,
            title = state.title,
            content = state.content,
            createdAt = state.createdAt,
            modifiedAt = modifiedAt,
            deletedAt = state.deletedAt
        )
        return try {
            if (state.isNew) {
                val newId = repository.insertNote(note)
                _uiState.update { it.copy(id = newId, isNew = false) }
            } else {
                repository.updateNote(note)
            }
            modifiedAt
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save note", e)
            null
        }
    }

    companion object {
        private const val TAG = "NoteDetailViewModel"
    }
}

enum class SavedState { Saved, Unsaved, Idle, Saving, Error }

data class NoteDetailUiState(
    val id: Long? = null,
    val folderId: Long? = null,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isNew: Boolean = true,
    val savedState: SavedState = SavedState.Idle,
    /**
     * Content seed passed through navigation from the list screen, so the editor's first frame
     * already shows the note instead of popping it in when the Room load lands mid-transition.
     * Null for new notes. Cleared implicitly once [content] is loaded — Room stays authoritative.
     */
    val initialContent: String? = null
)
