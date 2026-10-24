package com.instadoodle.app.image

import android.graphics.Bitmap
import com.instadoodle.app.drawing.PointF
import com.instadoodle.app.drawing.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.imgproc.Imgproc

/**
 * Extracts vector contours from preprocessed image Mats using Canny or Adaptive Thresholding.
 */
object ContourExtractor {

    enum class Mode {
        PHOTO,
        LOGO_LINE_ART
    }

    data class Config(
        val mode: Mode = Mode.PHOTO,
        // Canny parameters (Photo mode)
        val cannyThreshold1: Double = 50.0,
        val cannyThreshold2: Double = 150.0,
        val cannyApertureSize: Int = 3,
        val cannyL2Gradient: Boolean = true,
        // Adaptive Threshold parameters (Logo / Line art mode)
        val adaptiveBlockSize: Int = 11,
        val adaptiveC: Double = 2.0,
        val invertAdaptive: Boolean = true
    )

    data class ExtractionResult(
        val rawStrokes: List<Stroke>,
        val edgeBitmap: Bitmap,
        val width: Int,
        val height: Int
    )

    /**
     * Extracts contours from [preprocessedMat] on [Dispatchers.Default].
     */
    suspend fun extractContours(
        preprocessedMat: Mat,
        config: Config = Config()
    ): ExtractionResult = withContext(Dispatchers.Default) {
        val width = preprocessedMat.cols()
        val height = preprocessedMat.rows()

        val edgesMat = Mat()
        val hierarchyMat = Mat()
        val contourPoints = ArrayList<MatOfPoint>()

        try {
            when (config.mode) {
                Mode.PHOTO -> {
                    // Canny edge detection
                    Imgproc.Canny(
                        preprocessedMat,
                        edgesMat,
                        config.cannyThreshold1,
                        config.cannyThreshold2,
                        config.cannyApertureSize,
                        config.cannyL2Gradient
                    )
                }

                Mode.LOGO_LINE_ART -> {
                    // Adaptive thresholding
                    val threshType = if (config.invertAdaptive) {
                        Imgproc.THRESH_BINARY_INV
                    } else {
                        Imgproc.THRESH_BINARY
                    }
                    val blockSize = if (config.adaptiveBlockSize % 2 == 0) {
                        config.adaptiveBlockSize + 1
                    } else {
                        config.adaptiveBlockSize
                    }.coerceAtLeast(3)

                    Imgproc.adaptiveThreshold(
                        preprocessedMat,
                        edgesMat,
                        255.0,
                        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                        threshType,
                        blockSize,
                        config.adaptiveC
                    )
                }
            }

            // Generate edge preview bitmap
            val edgeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val edgeRgba = Mat()
            Imgproc.cvtColor(edgesMat, edgeRgba, Imgproc.COLOR_GRAY2RGBA)
            Utils.matToBitmap(edgeRgba, edgeBitmap)
            edgeRgba.release()

            // Find contours
            Imgproc.findContours(
                edgesMat,
                contourPoints,
                hierarchyMat,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_NONE
            )

            // Convert OpenCV MatOfPoint contours to Stroke data models
            val strokes = ArrayList<Stroke>(contourPoints.size)
            for (contour in contourPoints) {
                val pts = contour.toArray()
                if (pts.isNotEmpty()) {
                    val strokePoints = pts.map { PointF(it.x.toFloat(), it.y.toFloat()) }
                    strokes.add(Stroke(points = strokePoints, closed = true))
                }
                contour.release()
            }

            ExtractionResult(
                rawStrokes = strokes,
                edgeBitmap = edgeBitmap,
                width = width,
                height = height
            )
        } finally {
            edgesMat.release()
            hierarchyMat.release()
        }
    }
}
