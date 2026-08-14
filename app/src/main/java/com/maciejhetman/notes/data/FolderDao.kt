package com.maciejhetman.notes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE parentFolderId IS :parentFolderId ORDER BY name ASC")
    fun getSubfolders(parentFolderId: Long?): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    fun getFolderById(id: Long): Flow<Folder?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Delete
    suspend fun deleteFolder(folder: Folder)
}
