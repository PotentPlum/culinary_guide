package nl.potentplum.mise.ingest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import nl.potentplum.mise.core.util.IoDispatcher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/** Downscaling for stored images (spec §7.4): at most 1600 px on the long edge, JPEG quality 85, metadata dropped. */
object ImageProcessor {
    const val MAX_EDGE = 1600
    const val QUALITY = 85

    /** Claude vision's long-edge limit (spec §8.2). */
    const val VISION_MAX_EDGE = 1568

    /** The largest power-of-two subsample that still leaves the long edge ≥ [maxEdge]. */
    fun sampleSize(
        width: Int,
        height: Int,
        maxEdge: Int = MAX_EDGE,
    ): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    /** The final size, keeping the aspect ratio; images that already fit are left alone. */
    fun targetSize(
        width: Int,
        height: Int,
        maxEdge: Int = MAX_EDGE,
    ): Pair<Int, Int> {
        val longEdge = max(width, height)
        if (longEdge <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / longEdge
        return maxOf(1, (width * scale).roundToInt()) to maxOf(1, (height * scale).roundToInt())
    }

    /** Decodes, downscales and re-encodes as JPEG, which also drops EXIF; null when the bytes are not an image. */
    fun toJpeg(
        bytes: ByteArray,
        maxEdge: Int = MAX_EDGE,
    ): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge) }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val (width, height) = targetSize(decoded.width, decoded.height, maxEdge)
        val scaled =
            if (width == decoded.width &&
                height == decoded.height
            ) {
                decoded
            } else {
                Bitmap.createScaledBitmap(decoded, width, height, true)
            }
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
            out.toByteArray()
        }
    }
}

/** Local copies of recipe images and journal photos, so the library works offline (spec §6.9, §7.4). */
@Singleton
class ImageStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val client: OkHttpClient,
        @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    ) {
        fun recipeImage(recipeId: String): File = File(File(context.filesDir, "images"), "$recipeId.jpg")

        fun journalPhoto(attemptId: String): File = File(File(context.filesDir, "journal"), "$attemptId.jpg")

        /** Downloads and stores an imported recipe's image; null when it can't be fetched or decoded (never fatal). */
        suspend fun downloadRecipeImage(
            recipeId: String,
            url: String,
        ): String? =
            withContext(dispatcher) {
                val https = url.toHttpUrlOrNull()?.let(RecipeUrls::upgrade) ?: return@withContext null
                runCatching {
                    client.newCall(Request.Builder().url(https).build()).execute().use { response ->
                        if (!response.isSuccessful) return@runCatching null
                        val buffer = Buffer()
                        val source = response.body.source()
                        while (buffer.size <= MAX_DOWNLOAD_BYTES && source.read(buffer, 64 * 1024L) != -1L) Unit
                        if (buffer.size > MAX_DOWNLOAD_BYTES) return@runCatching null
                        store(recipeImage(recipeId), buffer.readByteArray())
                    }
                }.getOrNull()
            }

        /** Downscales [bytes] into [target]; returns its path, or null when the bytes are not an image. */
        fun store(
            target: File,
            bytes: ByteArray,
        ): String? {
            val jpeg = ImageProcessor.toJpeg(bytes) ?: return null
            target.parentFile?.mkdirs()
            val temp = File(target.parentFile, target.name + ".tmp")
            temp.writeBytes(jpeg)
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            return target.absolutePath
        }

        /** Photos for a photo import wait here until the recipe is saved (spec §6.9); the camera writes here too. */
        fun importDir(): File = File(context.cacheDir, "import").apply { mkdirs() }

        fun newImportFile(): File = File(importDir(), "${UUID.randomUUID()}.jpg")

        /**
         * Copies a picked or shared photo into the import folder, downscaled for Claude vision (≤ 1568 px, spec §8.2);
         * null when it can't be read or is not an image.
         */
        suspend fun importPhoto(uri: Uri): String? =
            withContext(dispatcher) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
                    downscaleForImport(newImportFile(), bytes)
                }.getOrNull()
            }

        /** Downscales a camera shot in place; deletes it when it is not a usable image. */
        suspend fun finishCameraPhoto(file: File): String? =
            withContext(dispatcher) {
                val result = runCatching { file.takeIf { it.length() > 0 }?.readBytes()?.let { downscaleForImport(file, it) } }.getOrNull()
                if (result == null) file.delete()
                result
            }

        private fun downscaleForImport(
            target: File,
            bytes: ByteArray,
        ): String? {
            val jpeg = ImageProcessor.toJpeg(bytes, ImageProcessor.VISION_MAX_EDGE) ?: return null
            target.writeBytes(jpeg)
            return target.absolutePath
        }

        fun deleteRecipeImage(recipeId: String) {
            recipeImage(recipeId).delete()
        }

        private companion object {
            const val MAX_DOWNLOAD_BYTES = 15L * 1024 * 1024
        }
    }
