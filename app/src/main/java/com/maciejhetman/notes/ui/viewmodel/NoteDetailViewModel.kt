package com.maciejhetman.notes.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.data.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class NoteDetailViewModel(
    private val repository: NoteRepository,
    private val noteId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState(id = noteId, isNew = noteId == null))
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        if (noteId != null) {
            viewModelScope.launch {
                val note = repository.getNoteStream(noteId).filterNotNull().first()
                _uiState.update {
                    it.copy(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        createdAt = note.createdAt,
                        modifiedAt = note.modifiedAt,
                        isNew = false,
                        savedState = SavedState.Saved
                    )
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
            if (performSave()) {
                _uiState.update { it.copy(savedState = SavedState.Saved) }
            }
        }
    }

    fun saveNote() {
        autoSaveJob?.cancel()
        // Nothing pending — the note was only opened/viewed, not edited. Skip the write
        // entirely so simply navigating into a note never bumps its modified timestamp.
        if (_uiState.value.savedState != SavedState.Unsaved) return
        viewModelScope.launch {
            if (performSave()) {
                _uiState.update { it.copy(savedState = SavedState.Saved) }
            }
        }
    }

    private suspend fun performSave(): Boolean {
        val state = _uiState.value
        // Don't save truly empty new notes
        if (state.isNew && state.title.isBlank() && state.content.isBlank()) return false

        val note = Note(
            id = state.id ?: 0,
            title = state.title,
            content = state.content,
            createdAt = state.createdAt,
            modifiedAt = System.currentTimeMillis()
        )
        return try {
            if (state.isNew) {
                val newId = repository.insertNote(note)
                _uiState.update { it.copy(id = newId, isNew = false) }
            } else {
                repository.updateNote(note)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save note", e)
            false
        }
    }

    companion object {
        private const val TAG = "NoteDetailViewModel"
    }
}

enum class SavedState { Saved, Unsaved, Idle }

data class NoteDetailUiState(
    val id: Long? = null,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isNew: Boolean = true,
    val savedState: SavedState = SavedState.Idle
)
