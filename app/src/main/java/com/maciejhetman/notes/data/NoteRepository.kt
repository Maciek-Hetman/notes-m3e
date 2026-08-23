package com.maciejhetman.notes.data

import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotesStream(): Flow<List<Note>>
    fun getNoteStream(id: Long): Flow<Note?>
    suspend fun insertNote(note: Note): Long
    suspend fun deleteNote(note: Note)
    suspend fun updateNote(note: Note)
    fun searchNotes(query: String): Flow<List<Note>>

    /**
     * Retrieve all notes from the given data source that belong to a specific folder.
     */
    fun getNotesStreamByFolderId(folderId: Long?): Flow<List<Note>>

    /** Retrieve all soft-deleted (trashed) notes. */
    fun getDeletedNotesStream(): Flow<List<Note>>

    /** Soft-delete [note]: it stays in the database but is hidden from the main lists. */
    suspend fun moveToTrash(note: Note)

    /** Bring a trashed note back to the active lists, keeping its content intact. */
    suspend fun restoreFromTrash(note: Note)
}
