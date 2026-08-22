package com.maciejhetman.notes.navigation

/**
 * Classifies the relationship between two navigation destinations so transition specs can be
 * chosen without comparing serialized route strings (which break for type-safe routes that carry
 * different arguments, e.g. two [Routes.NoteList] folder levels).
 */
enum class NavDestinationKind {
    NoteList,
    NoteDetail,
    Settings,
    Unknown,
}

enum class NavTransitionKind {
    /** Folder push/pop or list ↔ Settings — Android 17 Settings-style shared axis. */
    Hierarchical,

    /** List ↔ note detail — shared-element container transform; screens only fade. */
    ContainerTransform,

    /** Fallback for unrecognized pairs. */
    Fade,
}

fun navDestinationKind(route: String?): NavDestinationKind = when {
    route == null -> NavDestinationKind.Unknown
    // Type-safe kotlinx.serialization routes embed the FQCN / simple class name.
    route.contains("NoteDetail") -> NavDestinationKind.NoteDetail
    route.contains("Settings") -> NavDestinationKind.Settings
    route.contains("NoteList") -> NavDestinationKind.NoteList
    else -> NavDestinationKind.Unknown
}

fun navTransitionKind(
    from: NavDestinationKind,
    to: NavDestinationKind,
): NavTransitionKind = when {
    from == NavDestinationKind.NoteDetail || to == NavDestinationKind.NoteDetail ->
        NavTransitionKind.ContainerTransform

    (from == NavDestinationKind.NoteList && to == NavDestinationKind.NoteList) ||
        (from == NavDestinationKind.NoteList && to == NavDestinationKind.Settings) ||
        (from == NavDestinationKind.Settings && to == NavDestinationKind.NoteList) ->
        NavTransitionKind.Hierarchical

    else -> NavTransitionKind.Fade
}

fun navTransitionKind(fromRoute: String?, toRoute: String?): NavTransitionKind =
    navTransitionKind(navDestinationKind(fromRoute), navDestinationKind(toRoute))
