package com.maciejhetman.notes.ui.util

// Inline markdown image: ![alt](path) — group 1 is the image path/URI.
val IMAGE_MARKDOWN_REGEX = Regex("!\\[.*?\\]\\((.*?)\\)")
