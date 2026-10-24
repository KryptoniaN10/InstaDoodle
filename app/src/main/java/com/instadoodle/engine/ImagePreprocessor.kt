package com.instadoodle.engine

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/** OpenCV half of the pipeline: bitmap -> resized grayscale -> binary line image. */
object ImagePreprocessor {

    fun toBinary(bitmap: Bitmap, config: EngineConfig): BinaryImage {
        val mats = ArrayList<Mat>()
        fun mat() = Mat().also { mats += it }
        try {
            val rgba = mat()
            Utils.bitmapToMat(bitmap, rgba)
            val gray = mat()
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            val small = resize(gray, mat())
            val lines = when (config.mode) {
                SketchMode.PHOTO -> photoLines(small, config.detail, ::mat)
                SketchMode.LINE_ART -> lineArtLines(small, config.detail, ::mat)
            }
            val bytes = ByteArray((lines.total() * lines.channels()).toInt())
            lines.get(0, 0, bytes)
            return BinaryImage(lines.cols(), lines.rows(), BooleanArray(bytes.size) { bytes[it].toInt() != 0 })
        } finally {
            mats.forEach { it.release() }
        }
    }

    private fun resize(gray: Mat, out: Mat): Mat {
        val scale = EngineConfig.MAX_WORKING_SIDE.toDouble() / maxOf(gray.cols(), gray.rows())
        if (scale >= 1.0) return gray
        Imgproc.resize(gray, out, Size(gray.cols() * scale, gray.rows() * scale), 0.0, 0.0, Imgproc.INTER_AREA)
        return out
    }

    /**
     * Canny edges on an edge-preserving smoothed image. Unlike filled DoG blobs (which
     * thinning collapses to dots), Canny gives 1px contours, so facial features survive.
     * Thresholds follow the image median; lower detail = more blur and stricter thresholds.
     */
    private fun photoLines(gray: Mat, detail: Float, mat: () -> Mat): Mat {
        val smooth = mat()
        Imgproc.bilateralFilter(gray, smooth, 9, 50.0, 50.0)
        val blurred = mat()
        Imgproc.GaussianBlur(smooth, blurred, Size(0.0, 0.0), 0.7 + (1.0 - detail) * 1.8)
        val low = maxOf(6.0, (0.5 - 0.38 * detail) * median(blurred))
        val edges = mat()
        Imgproc.Canny(blurred, edges, low, low * 2.5, 3, true)
        return edges
    }

    private fun median(gray: Mat): Double {
        val bytes = ByteArray((gray.total() * gray.channels()).toInt())
        gray.get(0, 0, bytes)
        val histogram = IntArray(256)
        bytes.forEach { histogram[it.toInt() and 0xFF]++ }
        var seen = 0
        for (v in 0 until 256) {
            seen += histogram[v]
            if (seen * 2 >= bytes.size) return v.toDouble()
        }
        return 128.0
    }

    /**
     * Otsu threshold for logos/handwriting. Thin lines are kept as-is (thinning
     * centres them later); thick filled regions are replaced by their outline.
     */
    private fun lineArtLines(gray: Mat, detail: Float, mat: () -> Mat): Mat {
        val blurred = mat()
        Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)
        val bin = mat()
        Imgproc.threshold(blurred, bin, 0.0, 255.0, Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU)
        if (Core.countNonZero(bin) > bin.total() / 2) Core.bitwise_not(bin, bin)

        val k = (7 + ((1f - detail) * 6).toInt()) or 1
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(k.toDouble(), k.toDouble()))
        val small = Mat.ones(3, 3, CvType.CV_8U)
        val thick = mat()
        Imgproc.morphologyEx(bin, thick, Imgproc.MORPH_OPEN, kernel)
        val outline = mat()
        Imgproc.morphologyEx(thick, outline, Imgproc.MORPH_GRADIENT, small)
        val thickGrown = mat()
        Imgproc.dilate(thick, thickGrown, small)
        val lines = mat()
        Core.subtract(bin, thickGrown, lines)
        val result = mat()
        Core.bitwise_or(lines, outline, result)
        kernel.release()
        small.release()
        return result
    }
}
