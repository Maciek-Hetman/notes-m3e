package com.maciejhetman.notes

import android.app.Application
import com.maciejhetman.notes.data.NoteDatabase
import com.maciejhetman.notes.data.NoteRepository
import com.maciejhetman.notes.data.OfflineNoteRepository

class NotesApplication : Application() {
    val database: NoteDatabase by lazy { NoteDatabase.getDatabase(this) }
    val repository: NoteRepository by lazy { OfflineNoteRepository(database.noteDao()) }
}
