package com.maciejhetman.notes.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.ui.util.confirm
import com.maciejhetman.notes.ui.util.gestureThresholdActivate
import com.maciejhetman.notes.ui.util.longPress
import com.maciejhetman.notes.ui.util.reject
import com.maciejhetman.notes.ui.util.tap
import com.maciejhetman.notes.ui.viewmodel.DateRangeFilter
import com.maciejhetman.notes.ui.viewmodel.NoteListViewModel
import com.maciejhetman.notes.ui.viewmodel.SortOption
import com.maciejhetman.notes.ui.theme.LocalAppSettings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// SearchBar/InputField(query, expanded, onExpandedChange, ...) are deprecated in favor of the
// SearchBarState + TextFieldState slot API, but this bar never expands into a full-screen/docked
// results view, so migrating would require a state-management rewrite with no behavioral upside.
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteListViewModel,
    onNoteClick: (Long) -> Unit,
    onAddNoteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.notesUiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            onSearch = { },
                            expanded = false,
                            onExpandedChange = { },
                            placeholder = {
                                Text(
                                    "Search notes…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                // Crossfade between the two trailing actions so both always occupy
                                // the exact same slot as the built-in search-bar chrome — guarantees
                                // the settings icon is vertically centered exactly like Clear is.
                                Crossfade(targetState = searchQuery.isNotEmpty(), label = "search_trailing_icon") { hasQuery ->
                                    if (hasQuery) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                                        }
                                    } else {
                                        IconButton(onClick = onSettingsClick) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    },
                    expanded = false,
                    onExpandedChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
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
                        trailingIcon = if (uiState.dateRangeFilter != null) {
                            {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear date filter",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            haptics.tap()
                                            viewModel.onClearDateRangeFilter()
                                        }
                                )
                            }
                        } else null
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptics.tap()
                    onAddNoteClick()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.notes.isEmpty()) {
                // ── Empty state ──────────────────────────────────────────
                EmptyNotesPlaceholder(
                    isSearching = searchQuery.isNotEmpty() || uiState.dateRangeFilter != null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.notes,
                        key = { note -> note.id }
                    ) { note ->
                        SwipeableNoteItem(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onDismiss = { noteToDelete = note },
                            onDeleteClick = { noteToDelete = note }
                        )
                    }
                }
            }
        }
    }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.reject()
                        val note = noteToDelete
                        if (note != null) {
                            viewModel.deleteNote(note)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Note deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    haptics.confirm()
                                    viewModel.undoDelete(note)
                                }
                            }
                        }
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
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
}

private const val END_OF_DAY_OFFSET_MS = 24 * 60 * 60 * 1000L - 1L

// SimpleDateFormat is expensive to construct and not thread-safe to share across threads, but
// these two formatters are only ever touched from the main thread here, so caching one instance
// each avoids re-parsing the pattern string on every note item / date range render. The locale is
// intentionally captured once at startup; this app does not need to react to locale changes while
// running.
@Suppress("ConstantLocale")
private val MONTH_DAY_FORMATTER = SimpleDateFormat("MMM d", Locale.getDefault())

private val NOTE_PREVIEW_IMAGE_REGEX = Regex("!\\[.*?\\]\\((.*?)\\)")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilterDialog(
    initialFilter: DateRangeFilter?,
    onDismiss: () -> Unit,
    onConfirm: (DateRangeFilter) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFilter?.startInclusive,
        initialSelectedEndDateMillis = initialFilter?.endInclusive?.minus(END_OF_DAY_OFFSET_MS)
    )

    val currentDensity = LocalDensity.current

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    if (start != null && end != null) {
                        onConfirm(
                            DateRangeFilter(
                                startInclusive = start,
                                endInclusive = end + END_OF_DAY_OFFSET_MS
                            )
                        )
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = currentDensity.density * 0.82f,
                fontScale = currentDensity.fontScale * 0.82f
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(470.dp),
                title = {
                    Text(
                        text = "Filter notes by created date",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)
                    )
                }
            )
        }
    }
}

private fun formatDateRange(filter: DateRangeFilter): String {
    val start = MONTH_DAY_FORMATTER.format(Date(filter.startInclusive))
    val end = MONTH_DAY_FORMATTER.format(Date(filter.endInclusive))
    return if (start == end) start else "$start – $end"
}

// confirmValueChange is deprecated without a direct replacement; it is used here purely to trigger
// the delete-confirmation dialog and always veto the swipe (return false) so the item snaps back.
// The suggested anchors-based migration would change the swipe/settle mechanics, so it's deferred.
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNoteItem(
    note: Note,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.7f }, // less sensitive threshold
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd) {
                haptics.gestureThresholdActivate()
                onDismiss()
            }
            false // always snap back; deletion is handled by dialog confirmation
        }
    )

    val shape = RoundedCornerShape(16.dp)

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.errorContainer
                },
                animationSpec = tween(300),
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(color),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    ) {
        NoteItem(note = note, onClick = onClick, onDeleteClick = onDeleteClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val firstImagePath = remember(note.content) {
        NOTE_PREVIEW_IMAGE_REGEX.find(note.content)?.groupValues?.getOrNull(1)
    }
    var menuExpanded by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(16.dp)

    val appSettings = LocalAppSettings.current
    val fontFamily = appSettings.fontFamily.toComposeFontFamily()

    Box(modifier = modifier) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.longPress()
                        menuExpanded = true
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note.title.ifEmpty { "Untitled" },
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = formatTimestamp(note.modifiedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (note.content.isNotBlank()) {
                        val codeBackground = MaterialTheme.colorScheme.surfaceVariant
                        val cleanContent = remember(note.content, codeBackground) {
                            buildNotePreview(note.content, codeBackground)
                        }
                        if (cleanContent.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = cleanContent,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = fontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                if (firstImagePath != null) {
                    Card(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(model = firstImagePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete note") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    haptics.tap()
                    menuExpanded = false
                    onDeleteClick()
                }
            )
        }
    }
}


@Composable
private fun EmptyNotesPlaceholder(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NoteAlt,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (isSearching) "No notes found" else "Your notes will appear here",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isSearching) "Try a different search term"
                   else "Tap the + button to create your first note",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> MONTH_DAY_FORMATTER.format(Date(timestamp))
    }
}
