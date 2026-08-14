package com.maciejhetman.notes

import android.app.Application
import com.maciejhetman.notes.data.NoteDatabase
import com.maciejhetman.notes.data.NoteRepository
import com.maciejhetman.notes.data.OfflineNoteRepository
import com.maciejhetman.notes.data.FolderRepository
import com.maciejhetman.notes.data.OfflineFolderRepository
import com.maciejhetman.notes.data.SettingsRepository

class NotesApplication : Application() {
    val database: NoteDatabase by lazy { NoteDatabase.getDatabase(this) }
    val repository: NoteRepository by lazy { OfflineNoteRepository(database.noteDao()) }
    val folderRepository: FolderRepository by lazy { OfflineFolderRepository(database.folderDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
