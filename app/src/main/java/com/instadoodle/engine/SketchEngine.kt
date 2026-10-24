package com.instadoodle.engine

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader

/** User-facing failure with a friendly message. */
class SketchException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Entry point: bitmap -> ordered, normalized strokes. Heavy work runs on Dispatchers.Default. */
object SketchEngine {

    @Volatile
    private var openCvReady = false

    suspend fun createSketch(bitmap: Bitmap, config: EngineConfig): List<Stroke> = withContext(Dispatchers.Default) {
        ensureOpenCv()
        val binary = try {
            ImagePreprocessor.toBinary(bitmap, config)
        } catch (e: Exception) {
            throw SketchException("Image processing failed. Try a different image.", e)
        }
        if (binary.inkCount() == 0) {
            throw SketchException("No lines found in this image. Try the other mode or a higher detail level.")
        }
        val strokes = StrokeExtractor.extract(binary, config)
        if (strokes.isEmpty()) {
            throw SketchException("All lines were too short. Lower the minimum length or raise detail.")
        }
        strokes
    }

    private fun ensureOpenCv() {
        if (openCvReady) return
        openCvReady = OpenCVLoader.initLocal()
        if (!openCvReady) throw SketchException("The image engine (OpenCV) failed to load on this device.")
    }
}
