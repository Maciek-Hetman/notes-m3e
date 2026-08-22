package com.maciejhetman.notes.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes.
 */
object Routes {
    @Serializable
    data class NoteList(val folderId: Long? = null)

    @Serializable
    data class NoteDetail(
        val noteId: Long? = null,
        val folderId: Long? = null,
        /**
         * The note's content as already loaded by the list screen — used to seed the editor so
         * the first composed frame shows the full note instead of an empty field that pops in
         * mid-transition when the Room query lands. Not authoritative: Room still wins once
         * its stream emits.
         */
        val initialContent: String? = null
    )

    @Serializable
    data object Settings
}
