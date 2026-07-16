package com.maciejhetman.notes.navigation

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
import com.maciejhetman.notes.ui.viewmodel.NoteDetailViewModel
import com.maciejhetman.notes.ui.viewmodel.NoteListViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NotesNavHost() {
    val backStack = rememberNavBackStack(Destination.NoteList)
    val context = LocalContext.current
    val repository = (context.applicationContext as NotesApplication).repository

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
        onBack = { backStack.removeLastOrNull() },
        sceneStrategy = listDetailStrategy,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
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
                    onAddNoteClick = { backStack.add(Destination.NoteDetail()) }
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
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
