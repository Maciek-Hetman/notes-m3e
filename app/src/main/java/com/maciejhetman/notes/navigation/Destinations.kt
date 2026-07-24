package com.maciejhetman.notes.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable
    data object NoteList : Destination

    @Serializable
    data class NoteDetail(val noteId: Long? = null) : Destination

    @Serializable
    data object Settings : Destination
}
