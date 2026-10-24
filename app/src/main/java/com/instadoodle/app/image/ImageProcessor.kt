package com.instadoodle.app.image

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Handles image preprocessing: resizing, grayscale conversion, denoising, and CLAHE contrast enhancement.
 */
object ImageProcessor {

    private const val TAG = "ImageProcessor"

    @Volatile
    private var openCvLoaded: Boolean? = null

    /**
     * Ensures that native OpenCV libraries are loaded.
     */
    fun isAvailable(): Boolean {
        if (openCvLoaded == null) {
            synchronized(this) {
                if (openCvLoaded == null) {
                    openCvLoaded = try {
                        val success = OpenCVLoader.initLocal()
                        Log.i(TAG, "OpenCV initLocal result: $success")
                        success
                    } catch (t: Throwable) {
                        Log.e(TAG, "OpenCV native loading failed", t)
                        false
                    }
                }
            }
        }
        return openCvLoaded == true
    }

    data class PreprocessConfig(
        val maxDimension: Int = 512,
        val useBilateralFilter: Boolean = false,
        val gaussianBlurKernel: Int = 3,
        val claheClipLimit: Double = 2.5,
        val claheTileGridSize: Int = 8
    )

    /**
     * Preprocesses an input [Bitmap] on [Dispatchers.Default].
     *
     * Steps:
     * 1. Resizes with aspect ratio preserved so max dimension <= [PreprocessConfig.maxDimension]
     * 2. Converts to Grayscale
     * 3. Denoises using GaussianBlur or BilateralFilter
     * 4. Enhances local contrast with CLAHE
     *
     * @return Result containing preprocessed [Mat] and downscaled [Bitmap] preview.
     */
    suspend fun preprocess(
        inputBitmap: Bitmap,
        config: PreprocessConfig = PreprocessConfig()
    ): PreprocessResult = withContext(Dispatchers.Default) {
        check(isAvailable()) { "OpenCV is not initialized" }

        val origWidth = inputBitmap.width
        val origHeight = inputBitmap.height
        val maxDim = max(origWidth, origHeight)
        val scale = if (maxDim > config.maxDimension) {
            config.maxDimension.toDouble() / maxDim
        } else {
            1.0
        }

        val targetWidth = (origWidth * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (origHeight * scale).roundToInt().coerceAtLeast(1)

        val srcRgba = Mat()
        val resizedMat = Mat()
        val grayMat = Mat()
        val denoisedMat = Mat()
        val enhancedMat = Mat()

        try {
            // Step A.1: Bitmap to Mat
            Utils.bitmapToMat(inputBitmap, srcRgba)

            // Step A.2: Resize preserving aspect ratio
            if (scale != 1.0) {
                Imgproc.resize(
                    srcRgba,
                    resizedMat,
                    Size(targetWidth.toDouble(), targetHeight.toDouble()),
                    0.0,
                    0.0,
                    Imgproc.INTER_AREA
                )
            } else {
                srcRgba.copyTo(resizedMat)
            }

            // Step A.3: Grayscale conversion
            Imgproc.cvtColor(resizedMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            // Step A.4: Denoising
            if (config.useBilateralFilter) {
                Imgproc.bilateralFilter(grayMat, denoisedMat, 9, 75.0, 75.0)
            } else {
                val kernelSize = if (config.gaussianBlurKernel % 2 == 0) {
                    config.gaussianBlurKernel + 1
                } else {
                    config.gaussianBlurKernel
                }.coerceAtLeast(1).toDouble()

                Imgproc.GaussianBlur(grayMat, denoisedMat, Size(kernelSize, kernelSize), 0.0)
            }

            // Step A.5: CLAHE Contrast Enhancement
            val clahe = Imgproc.createCLAHE(
                config.claheClipLimit,
                Size(config.claheTileGridSize.toDouble(), config.claheTileGridSize.toDouble())
            )
            clahe.apply(denoisedMat, enhancedMat)

            // Convert enhanced Mat to preview bitmap
            val previewBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val previewRgba = Mat()
            Imgproc.cvtColor(enhancedMat, previewRgba, Imgproc.COLOR_GRAY2RGBA)
            Utils.matToBitmap(previewRgba, previewBitmap)
            previewRgba.release()

            PreprocessResult(
                enhancedMat = enhancedMat,
                previewBitmap = previewBitmap,
                width = targetWidth,
                height = targetHeight
            )
        } finally {
            srcRgba.release()
            resizedMat.release()
            grayMat.release()
            denoisedMat.release()
        }
    }

    class PreprocessResult(
        val enhancedMat: Mat,
        val previewBitmap: Bitmap,
        val width: Int,
        val height: Int
    ) : AutoCloseable {
        override fun close() {
            enhancedMat.release()
        }
    }
}
