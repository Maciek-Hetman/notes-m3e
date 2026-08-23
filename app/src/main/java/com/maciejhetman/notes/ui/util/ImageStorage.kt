package com.maciejhetman.notes.ui.util

import android.content.Context
import android.net.Uri
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val IMAGES_DIR = "note_images"

/**
 * Copies the content at [uri] into app-internal storage and returns the new file's absolute
 * path, or null on failure. The stream copy is blocking I/O, so it runs on [Dispatchers.IO] —
 * call this from a coroutine, never directly from the main thread.
 */
suspend fun copyUriToInternalStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val extension = when (context.contentResolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val dir = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
        val file = File(dir, "img_${UUID.randomUUID()}.$extension")
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

fun deleteInternalImage(path: String) {
    runCatching {
        val file = File(path).canonicalFile
        // Only remove files we created under note_images/, never arbitrary paths from markdown.
        if (file.parentFile?.name != IMAGES_DIR) return
        if (file.exists()) file.delete()
    }
}

fun deleteInternalImagesReferencedBy(content: String) {
    IMAGE_MARKDOWN_REGEX.findAll(content).forEach { match ->
        val path = match.groupValues.getOrNull(1).orEmpty()
        if (path.isNotBlank()) deleteInternalImage(path)
    }
}
