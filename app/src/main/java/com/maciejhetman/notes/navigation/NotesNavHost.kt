package com.maciejhetman.notes.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.maciejhetman.notes.NotesApplication
import com.maciejhetman.notes.ui.animation.Motion
import com.maciejhetman.notes.ui.animation.predictiveBackCorners
import com.maciejhetman.notes.ui.screens.NoteDetailScreen
import com.maciejhetman.notes.ui.screens.NoteListScreen
import com.maciejhetman.notes.ui.screens.SettingsScreen
import com.maciejhetman.notes.ui.viewmodel.NoteDetailViewModel
import com.maciejhetman.notes.ui.viewmodel.NoteListViewModel
import com.maciejhetman.notes.ui.viewmodel.SettingsViewModel

/** Scope of the destination currently animating, used by [predictiveBackCorners]. */
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Builds a single-purpose [ViewModelProvider.Factory] that always constructs [builder]'s result. */
private fun <T : ViewModel> viewModelFactory(builder: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = builder() as VM
    }

@Composable
fun NotesNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as NotesApplication
    val repository = application.repository
    val folderRepository = application.folderRepository
    val settingsRepository = application.settingsRepository
    val edgeTracker = rememberPredictiveBackEdgeTracker(navController)

    NavHost(
        navController = navController,
        startDestination = Routes.NoteList(),
        // System cross-activity predictive back look (video 1 on support-animations page +
        // Google's Progress-API guidance): the outgoing card shrinks to 90% around a pivot
        // biased toward the swipe edge and drifts only ~5% of the window toward that edge —
        // it never flies across the screen. The incoming screen sits static at full size
        // behind it and is merely revealed. Linear specs track the gesture one-to-one.
        popEnterTransition = { EnterTransition.None },
        popExitTransition = {
            val sign = edgeTracker.swipeSign
            slideOutHorizontally(Motion.predictiveBackSpec()) { fullWidth ->
                (sign * fullWidth / Motion.PREDICTIVE_BACK_MAX_X_SHIFT).toInt()
            } + scaleOut(
                targetScale = Motion.PREDICTIVE_BACK_SCALE,
                transformOrigin = TransformOrigin(
                    pivotFractionX = if (sign > 0f) 0.7f else 0.3f,
                    pivotFractionY = 0.5f,
                ),
                animationSpec = Motion.predictiveBackSpec(),
            ) + fadeOut(Motion.predictiveBackCommitSpec())
        },
    ) {

        composable<Routes.NoteList> { backStackEntry ->
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                Box(Modifier.fillMaxSize().predictiveBackCorners()) {
                    val route = backStackEntry.toRoute<Routes.NoteList>()
                    val viewModel: NoteListViewModel = viewModel(
                        key = "notelist_${route.folderId}",
                        factory = viewModelFactory {
                            NoteListViewModel(repository, folderRepository, route.folderId)
                        }
                    )
                    NoteListScreen(
                        viewModel = viewModel,
                        onNoteClick = { note ->
                            navController.navigate(
                                Routes.NoteDetail(
                                    note.id,
                                    route.folderId,
                                    initialContent = note.content
                                )
                            )
                        },
                        onAddNoteClick = {
                            navController.navigate(Routes.NoteDetail(null, route.folderId))
                        },
                        onFolderClick = { folderId ->
                            navController.navigate(Routes.NoteList(folderId))
                        },
                        onSettingsClick = { navController.navigate(Routes.Settings) },
                        onBack = { if (route.folderId != null) navController.popBackStack() }
                    )
                }
            }
        }

        composable<Routes.NoteDetail> { backStackEntry ->
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                Box(Modifier.fillMaxSize().predictiveBackCorners()) {
                    val route = backStackEntry.toRoute<Routes.NoteDetail>()
                    val viewModel: NoteDetailViewModel = viewModel(
                        key = "note_${route.noteId}",
                        factory = viewModelFactory {
                            NoteDetailViewModel(
                                repository,
                                route.noteId,
                                route.folderId,
                                route.initialContent
                            )
                        }
                    )
                    NoteDetailScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        composable<Routes.Settings> {
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                Box(Modifier.fillMaxSize().predictiveBackCorners()) {
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
