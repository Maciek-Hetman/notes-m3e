package com.maciejhetman.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maciejhetman.notes.data.Note
import com.maciejhetman.notes.ui.screens.buildNotePreview
import com.maciejhetman.notes.ui.theme.LocalAppSettings
import com.maciejhetman.notes.ui.theme.toComposeFontFamily
import com.maciejhetman.notes.ui.util.IMAGE_MARKDOWN_REGEX
import com.maciejhetman.notes.ui.util.gestureThresholdActivate
import com.maciejhetman.notes.ui.util.longPress
import com.maciejhetman.notes.ui.util.tap
import com.maciejhetman.notes.ui.viewmodel.DateRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val END_OF_DAY_OFFSET_MS = 24 * 60 * 60 * 1000L - 1L

val monthDayFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterDialog(
    initialFilter: DateRangeFilter?,
    modifier: Modifier = Modifier,
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

fun formatDateRange(filter: DateRangeFilter): String {
    val start = monthDayFormatter.format(Instant.ofEpochMilli(filter.startInclusive))
    val end = monthDayFormatter.format(Instant.ofEpochMilli(filter.endInclusive))
    return if (start == end) start else "$start – $end"
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableNoteItem(
    note: Note,
    modifier: Modifier = Modifier,
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
        modifier = modifier
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
        IMAGE_MARKDOWN_REGEX.find(note.content)?.groupValues?.getOrNull(1)
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
fun EmptyNotesPlaceholder(
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

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> monthDayFormatter.format(Instant.ofEpochMilli(timestamp))
    }
}
