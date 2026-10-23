package Sakuin.Project.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import Sakuin.Project.data.remote.ScanError
import Sakuin.Project.data.remote.ScanException
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Receipt photos on disk. A photo is first shrunk into the cache for scanning; only when the
 * expense is saved is it kept under `files/receipts/`, referenced by a path relative to `files/`.
 */
class ReceiptStore(private val context: Context) {
    private val receiptsDir get() = File(context.filesDir, RECEIPTS_DIR)
    private val scanDir get() = File(context.cacheDir, "scan")
    private val cameraDir get() = File(context.cacheDir, "camera")

    /** Where the in-app camera writes the next photo; it is shrunk into a scan copy right after. */
    fun cameraFile(): File {
        cameraDir.mkdirs()
        return File(cameraDir, "capture.jpg")
    }

    /** Copies the photo at [uri] upright, at most [MAX_SIDE] px on its long side, as JPEG. */
    suspend fun prepareForScan(uri: Uri): File = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        fun <T> read(block: (InputStream) -> T): T {
            val stream = resolver.openInputStream(uri) ?: throw ScanException(ScanError.Image, "Cannot open $uri")
            return stream.use(block)
        }

        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            read { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ScanException(ScanError.Image, "Not an image")
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_SIDE) }
            val decoded = read { BitmapFactory.decodeStream(it, null, options) }
                ?: throw ScanException(ScanError.Image, "Cannot decode")
            val rotation = runCatching { read(::exifRotation) }.getOrDefault(0)
            val (width, height) = scaledSize(decoded.width, decoded.height, MAX_SIDE)
            val matrix = Matrix().apply {
                postScale(width / decoded.width.toFloat(), height / decoded.height.toFloat())
                postRotate(rotation.toFloat())
            }
            val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            scanDir.mkdirs()
            val file = File(scanDir, "scan-${System.currentTimeMillis()}.jpg")
            file.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            if (upright !== decoded) upright.recycle()
            decoded.recycle()
            file
        } catch (e: ScanException) {
            throw e
        } catch (e: Exception) {
            // e.g. SecurityException or IOException from a provider that went away.
            throw ScanException(ScanError.Image, e.message, e)
        }
    }

    /** Moves a scanned photo into permanent storage; returns the path to save on the transaction. */
    suspend fun keep(scan: File): String = withContext(Dispatchers.IO) {
        receiptsDir.mkdirs()
        val target = File(receiptsDir, "receipt-${System.currentTimeMillis()}.jpg")
        // Copied rather than renamed: a moved cache file would still count as cache storage.
        scan.copyTo(target, overwrite = true)
        scan.delete()
        "$RECEIPTS_DIR/${target.name}"
    }

    fun resolve(path: String): File = File(context.filesDir, path)

    fun discard(file: File?) {
        file?.delete()
    }

    /**
     * Removes scan copies made before [startedAt] (an earlier run) and receipts no transaction
     * points to any more (e.g. deleted transactions). The camera folder holds a single photo that
     * the next capture overwrites.
     */
    suspend fun cleanUp(referenced: Collection<String>, startedAt: Long) = withContext(Dispatchers.IO) {
        scanDir.listFiles()?.forEach { if (it.lastModified() < startedAt) it.delete() }
        val keep = referenced.toSet()
        receiptsDir.listFiles()?.forEach { file ->
            // Files from this run may belong to an expense that is being saved right now.
            if ("$RECEIPTS_DIR/${file.name}" !in keep && file.lastModified() < startedAt) file.delete()
        }
    }

    companion object {
        const val RECEIPTS_DIR = "receipts"
        /** Long side sent to Gemini: enough for small receipt print, small enough to upload quickly. */
        const val MAX_SIDE = 1600
        private const val JPEG_QUALITY = 80
    }
}

/** Largest power-of-two subsampling that still leaves the long side at least [maxSide]. */
internal fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
    return sample
}

/** [width]×[height] scaled down so the long side is at most [maxSide], keeping the aspect ratio. */
internal fun scaledSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
    val longest = max(width, height)
    if (longest <= maxSide) return width to height
    val scale = maxSide.toFloat() / longest
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}

private fun exifRotation(stream: InputStream): Int =
    when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
        else -> 0
    }
