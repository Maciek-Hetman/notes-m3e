package com.maciejhetman.notes.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.maciejhetman.notes.data.AppSettings

/**
 * App-wide user preferences (font size, line numbering, etc.), provided once near the root of
 * the composition (see MainActivity) so any screen can read them without prop-drilling.
 */
val LocalAppSettings = compositionLocalOf { AppSettings() }
