package com.maciejhetman.notes.data

import kotlinx.coroutines.flow.Flow

class OfflineFolderRepository(private val folderDao: FolderDao) : FolderRepository {
    override fun getSubfoldersStream(parentFolderId: Long?): Flow<List<Folder>> = folderDao.getSubfolders(parentFolderId)
    override fun getFolderStream(id: Long): Flow<Folder?> = folderDao.getFolderById(id)
    override suspend fun insertFolder(folder: Folder): Long = folderDao.insertFolder(folder)
    override suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)
}
