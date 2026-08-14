package com.maciejhetman.notes.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.maciejhetman.notes.NotesApplication
import com.maciejhetman.notes.ui.screens.NoteDetailScreen
import com.maciejhetman.notes.ui.screens.NoteListScreen
import com.maciejhetman.notes.ui.screens.SettingsScreen
import com.maciejhetman.notes.ui.viewmodel.NoteDetailViewModel
import com.maciejhetman.notes.ui.viewmodel.NoteListViewModel
import com.maciejhetman.notes.ui.viewmodel.SettingsViewModel

// Shared timing for every nav transition below, tuned to feel snappy without being abrupt.
private const val NAV_ANIMATION_DURATION_MS = 320

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Builds a single-purpose [ViewModelProvider.Factory] that always constructs [builder]'s result. */
private fun <T : ViewModel> viewModelFactory(builder: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = builder() as VM
    }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NotesNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as NotesApplication
    val repository = application.repository
    val folderRepository = application.folderRepository
    val settingsRepository = application.settingsRepository

    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.NoteList()
            ) {
                composable<Routes.NoteList>(
                    enterTransition = { fadeIn(tween(NAV_ANIMATION_DURATION_MS)) },
                    exitTransition = { fadeOut(tween(NAV_ANIMATION_DURATION_MS)) },
                    popEnterTransition = { fadeIn(tween(NAV_ANIMATION_DURATION_MS)) },
                    popExitTransition = { fadeOut(tween(NAV_ANIMATION_DURATION_MS)) },
                ) { backStackEntry ->
                    CompositionLocalProvider(
                        LocalNavAnimatedVisibilityScope provides this@composable,
                    ) {
                        val route = backStackEntry.toRoute<Routes.NoteList>()
                        val viewModel: NoteListViewModel = viewModel(
                            key = "notelist_${route.folderId}",
                            factory = viewModelFactory { NoteListViewModel(repository, folderRepository, route.folderId) }
                        )
                        NoteListScreen(
                            viewModel = viewModel,
                            onNoteClick = { noteId -> navController.navigate(Routes.NoteDetail(noteId, route.folderId)) },
                            onAddNoteClick = { navController.navigate(Routes.NoteDetail(null, route.folderId)) },
                            onFolderClick = { folderId -> navController.navigate(Routes.NoteList(folderId)) },
                            onSettingsClick = { navController.navigate(Routes.Settings) },
                            onBack = { if (route.folderId != null) navController.popBackStack() }
                        )
                    }
                }
                
                composable<Routes.NoteDetail>(
                    enterTransition = { fadeIn(tween(NAV_ANIMATION_DURATION_MS)) },
                    exitTransition = { fadeOut(tween(NAV_ANIMATION_DURATION_MS)) },
                    popEnterTransition = { fadeIn(tween(NAV_ANIMATION_DURATION_MS)) },
                    popExitTransition = { fadeOut(tween(NAV_ANIMATION_DURATION_MS)) },
                ) { backStackEntry ->
                    CompositionLocalProvider(
                        LocalNavAnimatedVisibilityScope provides this@composable,
                    ) {
                        val route = backStackEntry.toRoute<Routes.NoteDetail>()
                        val viewModel: NoteDetailViewModel = viewModel(
                            key = "note_${route.noteId}",
                            factory = viewModelFactory { NoteDetailViewModel(repository, route.noteId, route.folderId) }
                        )
                        NoteDetailScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }

                composable<Routes.Settings>(
                    enterTransition = {
                        slideInVertically(tween(NAV_ANIMATION_DURATION_MS)) { fullHeight -> fullHeight / 8 } +
                            fadeIn(tween(NAV_ANIMATION_DURATION_MS))
                    },
                    exitTransition = { fadeOut(tween(NAV_ANIMATION_DURATION_MS / 2)) },
                    popEnterTransition = { fadeIn(tween(NAV_ANIMATION_DURATION_MS / 2)) },
                    popExitTransition = {
                        slideOutVertically(tween(NAV_ANIMATION_DURATION_MS)) { fullHeight -> fullHeight / 8 } +
                            fadeOut(tween(NAV_ANIMATION_DURATION_MS))
                    }
                ) {
                    CompositionLocalProvider(
                        LocalNavAnimatedVisibilityScope provides this@composable,
                    ) {
                        val viewModel: SettingsViewModel = viewModel(
                            factory = viewModelFactory { SettingsViewModel(settingsRepository) }
                        )
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
