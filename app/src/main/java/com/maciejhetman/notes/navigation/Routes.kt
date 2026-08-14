package com.maciejhetman.notes.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes.
 */
object Routes {
    @Serializable
    data object NoteList

    @Serializable
    data class NoteDetail(val noteId: Long? = null)

    @Serializable
    data object Settings
}
