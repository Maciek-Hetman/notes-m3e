package com.maciejhetman.notes.data

import kotlinx.coroutines.flow.Flow

class OfflineNoteRepository(private val noteDao: NoteDao) : NoteRepository {
    override fun getAllNotesStream(): Flow<List<Note>> = noteDao.getAllNotes()

    override fun getNotesStreamByFolderId(folderId: Long?): Flow<List<Note>> = noteDao.getNotesByFolderId(folderId)

    override fun getNoteStream(id: Long): Flow<Note?> = noteDao.getNoteById(id)

    override suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    override suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    override suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    override fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)
}
