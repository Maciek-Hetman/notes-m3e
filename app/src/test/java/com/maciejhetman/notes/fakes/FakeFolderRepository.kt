package com.maciejhetman.notes.fakes

import com.maciejhetman.notes.data.Folder
import com.maciejhetman.notes.data.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeFolderRepository(initialFolders: List<Folder> = emptyList()) : FolderRepository {
    private val folders = MutableStateFlow(initialFolders)
    private var nextId = (initialFolders.maxOfOrNull { it.id } ?: 0L) + 1

    override fun getSubfoldersStream(parentFolderId: Long?): Flow<List<Folder>> =
        folders.map { list -> list.filter { it.parentFolderId == parentFolderId }.sortedBy { it.name } }

    override fun getFolderStream(id: Long): Flow<Folder?> =
        folders.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun insertFolder(folder: Folder): Long {
        val id = if (folder.id == 0L) nextId++ else folder.id
        val stored = folder.copy(id = id)
        folders.update { list -> list.filterNot { it.id == id } + stored }
        return id
    }

    override suspend fun deleteFolder(folder: Folder) {
        folders.update { list -> list.filterNot { it.id == folder.id } }
    }
}
