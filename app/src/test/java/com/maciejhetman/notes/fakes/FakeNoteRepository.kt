package com.maciejhetman.notes.fakes

import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.data.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [NoteRepository] for JVM tests. Mirrors Room semantics: inserting a note with
 * `id = 0` auto-generates an id, inserting with a non-zero id keeps it (undo-delete),
 * and every mutation re-emits on the open flows.
 */
class FakeNoteRepository(initialNotes: List<Note> = emptyList()) : NoteRepository {

    private val notes = MutableStateFlow(initialNotes)
    private var nextId = (initialNotes.maxOfOrNull { it.id } ?: 0L) + 1

    val insertedNotes = mutableListOf<Note>()
    val updatedNotes = mutableListOf<Note>()
    val deletedNotes = mutableListOf<Note>()

    var lastSearchQuery: String? = null
        private set

    override fun getAllNotesStream(): Flow<List<Note>> = notes

    override fun getNoteStream(id: Long): Flow<Note?> =
        notes.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun insertNote(note: Note): Long {
        val id = if (note.id == 0L) nextId++ else note.id
        val stored = note.copy(id = id)
        insertedNotes += stored
        notes.update { list -> list.filterNot { it.id == id } + stored }
        return id
    }

    override suspend fun deleteNote(note: Note) {
        deletedNotes += note
        notes.update { list -> list.filterNot { it.id == note.id } }
    }

    override suspend fun updateNote(note: Note) {
        updatedNotes += note
        notes.update { list -> list.map { if (it.id == note.id) note else it } }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        lastSearchQuery = query
        return notes.map { list ->
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
            }
        }
    }
}
