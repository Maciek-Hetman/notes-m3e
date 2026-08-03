package com.maciejhetman.notes.ui.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Copies the content at [uri] into app-internal storage and returns the new file's absolute
 * path, or null on failure. The stream copy is blocking I/O, so it runs on [Dispatchers.IO] —
 * call this from a coroutine, never directly from the main thread.
 */
suspend fun copyUriToInternalStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
