package com.maciejhetman.notes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.maciejhetman.notes.data.Folder
import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.ui.animation.Motion
import com.maciejhetman.notes.ui.util.confirm
import com.maciejhetman.notes.ui.util.reject
import com.maciejhetman.notes.ui.util.tap
import com.maciejhetman.notes.ui.viewmodel.NoteListViewModel
import com.maciejhetman.notes.ui.viewmodel.SortOption
import kotlinx.coroutines.launch
import com.maciejhetman.notes.ui.components.DateRangeFilterDialog
import com.maciejhetman.notes.ui.components.EmptyNotesPlaceholder
import com.maciejhetman.notes.ui.components.FolderItem
import com.maciejhetman.notes.ui.components.SwipeableNoteItem
import com.maciejhetman.notes.ui.components.TrashNoteItem
import com.maciejhetman.notes.ui.components.formatDateRange
import com.maciejhetman.notes.ui.viewmodel.ListSection

// SearchBar/InputField(query, expanded, onExpandedChange, ...) are deprecated in favor of the
// SearchBarState + TextFieldState slot API, but this bar never expands into a full-screen/docked
// results view, so migrating would require a state-management rewrite with no behavioral upside.
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    onNoteClick: (Note) -> Unit,
    onAddNoteClick: (initialContent: String?) -> Unit,
    onFolderClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.notesUiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToPurge by remember { mutableStateOf<Note?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var isFabExpanded by remember { mutableStateOf(false) }
    val folderNameFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = isFabExpanded) { isFabExpanded = false }

    val motionScheme = MaterialTheme.motionScheme
    val itemFadeInSpec = motionScheme.fastEffectsSpec<Float>()
    val itemPlacementSpec = Motion.listItemPlacementSpec<IntOffset>()
    val itemFadeOutSpec = motionScheme.fastEffectsSpec<Float>()
    val effectsSpec = motionScheme.fastEffectsSpec<Float>()
    val spatialSpec = motionScheme.fastSpatialSpec<Float>()
    val slideSpec = motionScheme.fastSpatialSpec<IntOffset>()

    val isInTrash = uiState.section == ListSection.DELETED
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
                )
                ListSection.entries.forEach { section ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = section.label,
                                fontWeight = if (section == uiState.section) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = section == uiState.section,
                        onClick = {
                            haptics.tap()
                            viewModel.onSectionChange(section)
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                imageVector = when (section) {
                                    ListSection.NOTES -> Icons.Default.NoteAlt
                                    ListSection.TODOS -> Icons.Default.Checklist
                                    ListSection.DELETED -> Icons.Default.DeleteOutline
                                },
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column(
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                if (viewModel.isInFolder) {
                                    onBack()
                                } else {
                                    haptics.tap()
                                    scope.launch { drawerState.open() }
                                }
                            }) {
                                Icon(
                                    if (viewModel.isInFolder) Icons.Default.ArrowBack else Icons.Default.Menu,
                                    contentDescription = if (viewModel.isInFolder) "Back" else "Sections",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        title = {
                            SearchBar(
                                inputField = {
                                    SearchBarDefaults.InputField(
                                        query = searchQuery,
                                        onQueryChange = {
                                            viewModel.onSearchQueryChange(it)
                                        },
                                        onSearch = { },
                                        expanded = false,
                                        onExpandedChange = { },
                                        placeholder = {
                                            Text(
                                                if (viewModel.isInFolder) "Search in this folder"
                                                else "Search notes"
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, contentDescription = null)
                                        },
                                        trailingIcon = {
                                            Crossfade(
                                                targetState = searchQuery.isNotEmpty(),
                                                animationSpec = effectsSpec,
                                                label = "search_trailing_icon"
                                            ) { hasQuery ->
                                                if (hasQuery) {
                                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Clear search"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                },
                                expanded = false,
                                onExpandedChange = { },
                                modifier = Modifier.fillMaxWidth()
                            ) { }
                        },
                        actions = {
                            IconButton(
                                onClick = onSettingsClick,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        FilterChip(
                            selected = uiState.sortOption != SortOption.MODIFIED_NEWEST,
                            onClick = {
                                haptics.tap()
                                sortMenuExpanded = true
                            },
                            label = {
                                Text(
                                    text = uiState.sortOption.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (option == uiState.sortOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (option == uiState.sortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = if (option == uiState.sortOption) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        haptics.tap()
                                        viewModel.onSortOptionChange(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    FilterChip(
                        selected = uiState.dateRangeFilter != null,
                        onClick = {
                            haptics.tap()
                            showDateRangePicker = true
                        },
                        label = {
                            Text(
                                text = uiState.dateRangeFilter?.let { formatDateRange(it) } ?: "Date range",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                    )
                    if (uiState.dateRangeFilter != null) {
                        IconButton(
                            onClick = {
                                haptics.tap()
                                viewModel.onClearDateRangeFilter()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear date filter",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            },
            floatingActionButton = {
            if (!isInTrash) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = fadeIn(effectsSpec) +
                            slideInVertically(animationSpec = slideSpec) { it / 2 },
                        exit = fadeOut(effectsSpec) +
                            slideOutVertically(animationSpec = slideSpec) { it / 2 }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onAddNoteClick("- [ ] ")
                                },
                                icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                                text = { Text("To-do note") },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                            ExtendedFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    showCreateFolderDialog = true
                                },
                                icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                text = { Text("Folder") },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                            ExtendedFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onAddNoteClick(null)
                                },
                                icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                                text = { Text("Note") },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = !isFabExpanded
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        AnimatedContent(
                            targetState = isFabExpanded,
                            label = "fab_icon",
                            transitionSpec = {
                                (scaleIn(
                                    animationSpec = spatialSpec,
                                    initialScale = 0.6f
                                ) + fadeIn(effectsSpec)) togetherWith
                                    (scaleOut(
                                        animationSpec = spatialSpec,
                                        targetScale = 0.6f
                                    ) + fadeOut(effectsSpec))
                            }
                        ) { expanded ->
                            Icon(if (expanded) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            }
            }
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                // Show nothing while loading to avoid flash
            } else if (uiState.notes.isEmpty() && uiState.folders.isEmpty()) {
                // ── Empty state ──────────────────────────────────────────
                val showSectionEmptyState = searchQuery.isEmpty() && uiState.dateRangeFilter == null
                EmptyNotesPlaceholder(
                    isSearching = searchQuery.isNotEmpty() || uiState.dateRangeFilter != null,
                    title = when {
                        isInTrash && showSectionEmptyState -> "Trash is empty"
                        uiState.section == ListSection.TODOS && showSectionEmptyState -> "No to-do notes"
                        else -> null
                    },
                    message = when {
                        isInTrash && showSectionEmptyState ->
                            "Notes you delete will appear here"
                        uiState.section == ListSection.TODOS && showSectionEmptyState ->
                            "Notes containing to-do checkboxes will appear here"
                        else -> null
                    },
                    icon = when {
                        isInTrash && showSectionEmptyState -> Icons.Default.DeleteOutline
                        uiState.section == ListSection.TODOS && showSectionEmptyState -> Icons.Default.Checklist
                        else -> Icons.Default.NoteAlt
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 0.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (viewModel.isInFolder) "Folder notes" else uiState.section.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.folders.size + uiState.notes.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(
                        items = uiState.folders,
                        key = { folder -> "folder_${folder.id}" }
                    ) { folder ->
                        FolderItem(
                            folder = folder,
                            onClick = { onFolderClick(folder.id) },
                            onDeleteClick = { folderToDelete = folder },
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = itemFadeInSpec,
                                    placementSpec = itemPlacementSpec,
                                    fadeOutSpec = itemFadeOutSpec
                                )
                                .padding(horizontal = 16.dp)
                        )
                    }
                    items(
                        items = uiState.notes,
                        key = { note -> "note_${note.id}" }
                    ) { note ->
                        if (isInTrash) {
                            TrashNoteItem(
                                note = note,
                                onRestore = {
                                    haptics.confirm()
                                    viewModel.restoreNote(note)
                                },
                                onDeleteForever = { noteToPurge = note },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = itemFadeInSpec,
                                    placementSpec = itemPlacementSpec,
                                    fadeOutSpec = itemFadeOutSpec
                                )
                            )
                        } else {
                            SwipeableNoteItem(
                                note = note,
                                onClick = { onNoteClick(note) },
                                onDismiss = { noteToDelete = note },
                                onDeleteClick = { noteToDelete = note },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = itemFadeInSpec,
                                    placementSpec = itemPlacementSpec,
                                    fadeOutSpec = itemFadeOutSpec
                                )
                            )
                        }
                    }
                }
            }
            if (isFabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable { isFabExpanded = false }
                )
            }
        }
        }
    }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Move to trash?") },
            text = { Text("You can restore it from the Deleted section.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.tap()
                        val note = noteToDelete
                        if (note != null) {
                            viewModel.deleteNote(note)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Note moved to trash",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    haptics.confirm()
                                    viewModel.restoreNote(note)
                                }
                            }
                        }
                        noteToDelete = null
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (noteToPurge != null) {
        AlertDialog(
            onDismissRequest = { noteToPurge = null },
            title = { Text("Delete forever?") },
            text = { Text("This note will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.reject()
                        val note = noteToPurge
                        if (note != null) {
                            viewModel.permanentlyDeleteNote(note)
                        }
                        noteToPurge = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToPurge = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDateRangePicker) {
        DateRangeFilterDialog(
            initialFilter = uiState.dateRangeFilter,
            onDismiss = { showDateRangePicker = false },
            onConfirm = { filter ->
                haptics.confirm()
                viewModel.onDateRangeFilterChange(filter)
                showDateRangePicker = false
            }
        )
    }
    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete folder?") },
            text = { Text("This folder and all notes inside it will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.reject()
                        val folder = folderToDelete
                        if (folder != null) {
                            viewModel.deleteFolder(folder)
                        }
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        LaunchedEffect(Unit) {
            folderNameFocusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                newFolderName = ""
            },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newFolderName.isNotBlank()) {
                                viewModel.createFolder(newFolderName.trim())
                                showCreateFolderDialog = false
                                newFolderName = ""
                            }
                        }
                    ),
                    modifier = Modifier.focusRequester(folderNameFocusRequester)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                        }
                        showCreateFolderDialog = false
                        newFolderName = ""
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateFolderDialog = false
                        newFolderName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


