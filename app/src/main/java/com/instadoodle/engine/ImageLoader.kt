package com.instadoodle.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

/** Decodes user images safely: downsamples huge files, applies EXIF rotation, flattens alpha. */
object ImageLoader {

    private const val MAX_DECODE_SIDE = 2048

    fun fromUri(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        return decode(
            open = { resolver.openInputStream(uri) ?: throw SketchException("Could not open the selected image.") },
        )
    }

    fun fromAsset(context: Context, path: String): Bitmap =
        decode(open = { context.assets.open(path) })

    private fun decode(open: () -> InputStream): Bitmap {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open().use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw SketchException("This file is not a supported image.")

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = open().use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw SketchException("This image could not be decoded.")
            val orientation = runCatching {
                open().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            return flattenOnWhite(rotate(decoded, orientation))
        } catch (e: SketchException) {
            throw e
        } catch (e: OutOfMemoryError) {
            throw SketchException("This image is too large to process.", e)
        } catch (e: Exception) {
            throw SketchException("Could not read this image (${e.javaClass.simpleName}).", e)
        }
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= MAX_DECODE_SIDE) sample *= 2
        return sample
    }

    private fun rotate(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Transparent pixels would read as black; composite onto white first. */
    private fun flattenOnWhite(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha() && bitmap.config == Bitmap.Config.ARGB_8888) return bitmap
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        return out
    }
}
