package com.maciejhetman.notes.data

import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getSubfoldersStream(parentFolderId: Long?): Flow<List<Folder>>
    fun getFolderStream(id: Long): Flow<Folder?>
    suspend fun insertFolder(folder: Folder): Long
    suspend fun deleteFolder(folder: Folder)
}
