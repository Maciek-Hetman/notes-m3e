package com.maciejhetman.notes.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentFolderId")]
)
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentFolderId: Long? = null,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
