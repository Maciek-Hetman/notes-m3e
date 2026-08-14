package com.maciejhetman.notes.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes.
 */
object Routes {
    @Serializable
    data class NoteList(val folderId: Long? = null)

    @Serializable
    data class NoteDetail(val noteId: Long? = null, val folderId: Long? = null)

    @Serializable
    data object Settings
}
