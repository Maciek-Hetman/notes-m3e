package com.maciejhetman.notes.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.maciejhetman.notes.ui.components.MarkdownToolbar
import com.maciejhetman.notes.ui.components.NoteContentEditor
import com.maciejhetman.notes.ui.components.buildInsertedValue
import com.maciejhetman.notes.ui.components.rememberNoteEditorState
import com.maciejhetman.notes.ui.theme.LocalAppSettings
import com.maciejhetman.notes.ui.theme.isAppDarkTheme
import com.maciejhetman.notes.ui.theme.toComposeFontFamily
import com.maciejhetman.notes.ui.util.IMAGE_MARKDOWN_REGEX
import com.maciejhetman.notes.ui.util.copyUriToInternalStorage
import com.maciejhetman.notes.ui.viewmodel.NoteDetailViewModel
import com.maciejhetman.notes.ui.viewmodel.SavedState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentFocusRequester = remember { FocusRequester() }
    val editorState = rememberNoteEditorState(uiState.content)

    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // The copy is blocking I/O — run it in a coroutine (copyUriToInternalStorage
                // switches to Dispatchers.IO internally) instead of freezing the picker callback.
                scope.launch {
                    val savedPath = copyUriToInternalStorage(context, uri)
                    if (savedPath != null) {
                        val syntax = "\n![image]($savedPath)\n"
                        val (newValue) = buildInsertedValue(syntax, editorState.contentFieldValue)
                        editorState.contentFieldValue = newValue
                        viewModel.updateContent(newValue.text)
                        contentFocusRequester.requestFocus()
                        keyboardController?.show()
                    } else {
                        Toast.makeText(context, "Couldn't import image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val dateFormatter = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val timeFormatter = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    val createdStr = remember(uiState.createdAt) { dateFormatter.format(java.util.Date(uiState.createdAt)) }
    val modifiedStr = remember(uiState.modifiedAt) { timeFormatter.format(java.util.Date(uiState.modifiedAt)) }

    // Sync with Room on first load (existing note)
    LaunchedEffect(uiState.content) {
        if (editorState.contentFieldValue.text != uiState.content) {
            editorState.contentFieldValue = TextFieldValue(uiState.content, TextRange(uiState.content.length))
        }
    }

    // "Saved" flash indicator
    var showSavedIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.savedState) {
        if (uiState.savedState == SavedState.Saved) {
            showSavedIndicator = true
            delay(2.seconds)
            showSavedIndicator = false
        }
    }

    // Save on dispose (navigation away) AND on ON_STOP (app backgrounded / process death).
    // DisposableEffect alone is unreliable when the OS kills the process while backgrounded;
    // the LifecycleEventObserver fires on ON_STOP before that can happen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.saveNote()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.saveNote()
        }
    }

    // Colors needed by the visual transformation
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // User preferences (font size, line numbering, syntax theme, font family) — provided app-wide from Settings.
    val appSettings = LocalAppSettings.current
    val fontScale = appSettings.fontSizeScale
    val fontFamily = appSettings.fontFamily.toComposeFontFamily()

    val isDark = isAppDarkTheme(appSettings.themeMode)

    val syntaxColors = resolveSyntaxThemeColors(
        theme = appSettings.syntaxTheme,
        isDark = isDark,
        fallbackPrimary = primaryColor,
        fallbackSecondary = secondaryColor,
        fallbackTertiary = tertiaryColor,
        fallbackOnSurface = onSurfaceColor,
        fallbackSurfaceVariant = surfaceVariant
    )

    // Width reserved for line-number digits; kept in both Dp (for the overlay Text below) and
    // Sp (for the transformation's paragraph indent) so the two line up pixel-for-pixel.
    val gutterWidthDp = (36f * fontScale).dp
    val gutterWidthSp = with(density) { gutterWidthDp.toSp() }

    // Real rendered width of the content field — used so the reserved height for inline
    // images matches their actual displayed width/aspect-ratio (otherwise they look squashed).
    val containerWidthDp = with(density) { editorState.containerWidthPx.toDp().value }

    // Derive a stable boolean: is the cursor currently inside a markdown image span?
    // Using this instead of the full TextRange avoids recreating the transformation on every
    // cursor movement — it only changes when the cursor enters or leaves an image.
    val cursorInsideImage = remember(editorState.contentFieldValue.text, editorState.contentFieldValue.selection) {
        IMAGE_MARKDOWN_REGEX.findAll(editorState.contentFieldValue.text).any { match ->
            editorState.contentFieldValue.selection.start in match.range.first..(match.range.last + 1)
        }
    }

    // Recreated only when theme colors, user preferences, or image-related state actually change
    val markdownTransformation = remember(
        primaryColor, onSurfaceColor, surfaceVariant, cursorInsideImage, editorState.contentFieldValue.selection,
        editorState.imageAspectRatios.toMap(), containerWidthDp, fontScale, fontFamily,
        appSettings.lineNumberMode, gutterWidthSp, syntaxColors
    ) {
        MarkdownVisualTransformation(
            primaryColor = primaryColor,
            onSurfaceColor = onSurfaceColor,
            codeBackground = surfaceVariant,
            selection = editorState.contentFieldValue.selection,
            imageAspectRatios = editorState.imageAspectRatios,
            containerWidthDp = containerWidthDp,
            customHighlightColors = syntaxColors,
            fontFamily = fontFamily,
            fontScale = fontScale,
            lineNumberMode = appSettings.lineNumberMode,
            gutterWidth = gutterWidthSp
        )
    }

    // ── UI ─────────────────────────────────────────────────────────────────

    Scaffold(
        floatingActionButton = {
            MarkdownToolbar(
                onInsert = { syntax ->
                    val (newValue) = buildInsertedValue(syntax, editorState.contentFieldValue)
                    editorState.contentFieldValue = newValue
                    viewModel.updateContent(newValue.text)
                    // Toolbar buttons steal focus from the text field, which dismisses the
                    // keyboard — explicitly refocus and re-show it so typing can continue.
                    contentFocusRequester.requestFocus()
                    keyboardController?.show()
                },
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier.imePadding()
            )
        },
        floatingActionButtonPosition = FabPosition.Center

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // ── Top bar (scrolls away with the rest of the content) ────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.saveNote(); onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(4.dp))
                AnimatedVisibility(
                    visible = showSavedIndicator,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it }
                ) {
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Title ─────────────────────────────────────────────────────
            BasicTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { contentFocusRequester.requestFocus() }
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (uiState.title.isEmpty()) {
                            Text(
                                "Title",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Text(
                text = "Created $createdStr  •  Modified $modifiedStr",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // ── Content — live markdown via VisualTransformation ───────────
            NoteContentEditor(
                state = editorState,
                onContentChange = { viewModel.updateContent(it) },
                contentFocusRequester = contentFocusRequester,
                visualTransformation = markdownTransformation,
                appSettings = appSettings,
                fontFamily = fontFamily,
                syntaxColors = syntaxColors,
                fallbackCodeBackground = surfaceVariant,
                gutterWidthDp = gutterWidthDp
            )

            // Tapping the empty area below the content moves cursor to the end
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        contentFocusRequester.requestFocus()
                        editorState.contentFieldValue = TextFieldValue(
                            editorState.contentFieldValue.text,
                            TextRange(editorState.contentFieldValue.text.length)
                        )
                    }
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}
