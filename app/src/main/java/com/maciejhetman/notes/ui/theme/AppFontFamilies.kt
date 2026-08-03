package com.maciejhetman.notes.ui.theme

import androidx.compose.ui.text.font.FontFamily
import com.maciejhetman.notes.data.NoteFontFamily

// Extension rather than a member of the data-layer enum, so the data layer
// stays free of Compose dependencies.
fun NoteFontFamily.toComposeFontFamily(): FontFamily = when (this) {
    NoteFontFamily.SYSTEM -> FontFamily.Default
    NoteFontFamily.SANS_SERIF -> FontFamily.SansSerif
    NoteFontFamily.SERIF -> FontFamily.Serif
    NoteFontFamily.MONOSPACE -> FontFamily.Monospace
    NoteFontFamily.CURSIVE -> FontFamily.Cursive
}
