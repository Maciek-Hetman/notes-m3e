package com.maciejhetman.notes.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.maciejhetman.notes.NotesApplication
import com.maciejhetman.notes.ui.screens.NoteDetailScreen
import com.maciejhetman.notes.ui.screens.NoteListScreen
import com.maciejhetman.notes.ui.screens.SettingsScreen
import com.maciejhetman.notes.ui.viewmodel.NoteDetailViewModel
import com.maciejhetman.notes.ui.viewmodel.NoteListViewModel
import com.maciejhetman.notes.ui.viewmodel.SettingsViewModel

// Shared timing for every nav transition below, tuned to feel snappy without being abrupt.
private const val NAV_ANIMATION_DURATION_MS = 320

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NotesNavHost() {
    val backStack = rememberNavBackStack(Destination.NoteList)
    val context = LocalContext.current
    val application = context.applicationContext as NotesApplication
    val repository = application.repository
    val settingsRepository = application.settingsRepository

    @Suppress("DEPRECATION")
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    @Suppress("UNCHECKED_CAST")
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        sceneStrategies = listOf(listDetailStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        // Material "shared axis" (X) — the pattern most native Android apps (Settings, Gmail,
        // system apps) use for hierarchical navigation: a small horizontal shift combined with a
        // cross-fade, rather than a full off-screen slide. Forward: new content enters from a bit
        // right of center while the old content shifts left as it fades.
        transitionSpec = {
            (
                slideInHorizontally(tween(NAV_ANIMATION_DURATION_MS)) { fullWidth -> fullWidth / 10 } +
                    fadeIn(tween(NAV_ANIMATION_DURATION_MS))
            ) togetherWith (
                slideOutHorizontally(tween(NAV_ANIMATION_DURATION_MS)) { fullWidth -> -fullWidth / 10 } +
                    fadeOut(tween(NAV_ANIMATION_DURATION_MS))
            )
        },
        // Mirror image of the push above for backward navigation.
        popTransitionSpec = {
            (
                slideInHorizontally(tween(NAV_ANIMATION_DURATION_MS)) { fullWidth -> -fullWidth / 10 } +
                    fadeIn(tween(NAV_ANIMATION_DURATION_MS))
            ) togetherWith (
                slideOutHorizontally(tween(NAV_ANIMATION_DURATION_MS)) { fullWidth -> fullWidth / 10 } +
                    fadeOut(tween(NAV_ANIMATION_DURATION_MS))
            )
        },
        // Predictive back (Android 13+/14+ system gesture): the current screen shrinks and fades
        // slightly in place — no slide — so the revealed previous screen underneath reads as a
        // live preview that tracks the swipe, matching the system's own predictive-back affordance.
        predictivePopTransitionSpec = {
            fadeIn(tween(NAV_ANIMATION_DURATION_MS)) togetherWith
                (fadeOut(tween(NAV_ANIMATION_DURATION_MS)) + scaleOut(targetScale = 0.92f))
        },
        entryProvider = entryProvider {
            entry<Destination.NoteList>(
                metadata = ListDetailSceneStrategy.listPane()
            ) {
                val viewModel: NoteListViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return NoteListViewModel(repository) as T
                        }
                    }
                )
                NoteListScreen(
                    viewModel = viewModel,
                    onNoteClick = { noteId -> backStack.add(Destination.NoteDetail(noteId)) },
                    onAddNoteClick = { backStack.add(Destination.NoteDetail()) },
                    onSettingsClick = { backStack.add(Destination.Settings) }
                )
            }
            entry<Destination.Settings>(
                // Settings is reached via a gear icon rather than drilling into content, so it
                // gets a distinct vertical shared-axis (Y) transition instead of the horizontal one.
                metadata = NavDisplay.transitionSpec {
                    (
                        slideInVertically(tween(NAV_ANIMATION_DURATION_MS)) { fullHeight -> fullHeight / 8 } +
                            fadeIn(tween(NAV_ANIMATION_DURATION_MS))
                    ) togetherWith fadeOut(tween(NAV_ANIMATION_DURATION_MS / 2))
                } + NavDisplay.popTransitionSpec {
                    fadeIn(tween(NAV_ANIMATION_DURATION_MS / 2)) togetherWith (
                        slideOutVertically(tween(NAV_ANIMATION_DURATION_MS)) { fullHeight -> fullHeight / 8 } +
                            fadeOut(tween(NAV_ANIMATION_DURATION_MS))
                    )
                }
            ) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return SettingsViewModel(settingsRepository) as T
                        }
                    }
                )
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                )
            }
            entry<Destination.NoteDetail>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { key ->
                val viewModel: NoteDetailViewModel = viewModel(
                    key = "note_${key.noteId}",
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return NoteDetailViewModel(repository, key.noteId) as T
                        }
                    }
                )
                NoteDetailScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                )
            }
        }
    )
}
