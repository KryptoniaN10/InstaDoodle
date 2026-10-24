package com.instadoodle.engine

import java.io.File

/**
 * JVM-only helpers: OpenCV's Android build cannot run on the host JVM, so tests load the
 * bundled samples with ImageIO and binarize with a plain Otsu threshold before running the
 * pure-Kotlin half of the pipeline (thinning, tracing, simplification, filtering, ordering).
 * ImageIO is reached via reflection because android.jar hides javax.imageio at compile time;
 * it is present in the host JDK at test runtime.
 */
object TestImages {

    private val samplesDir = listOf("src/main/assets/samples", "app/src/main/assets/samples")
        .map(::File).first { it.isDirectory }

    fun loadGray(name: String): Triple<Int, Int, IntArray> {
        val src = Class.forName("javax.imageio.ImageIO").getMethod("read", File::class.java)
            .invoke(null, File(samplesDir, name)) ?: error("Cannot read $name")
        val srcW = src.javaClass.getMethod("getWidth").invoke(src) as Int
        val srcH = src.javaClass.getMethod("getHeight").invoke(src) as Int
        val getRgb = src.javaClass.getMethod("getRGB", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val scale = minOf(1.0, EngineConfig.MAX_WORKING_SIDE.toDouble() / maxOf(srcW, srcH))
        val w = (srcW * scale).toInt()
        val h = (srcH * scale).toInt()
        val gray = IntArray(w * h) { i ->
            val rgb = getRgb.invoke(src, ((i % w) / scale).toInt(), ((i / w) / scale).toInt()) as Int
            ((rgb shr 16 and 0xFF) * 299 + (rgb shr 8 and 0xFF) * 587 + (rgb and 0xFF) * 114) / 1000
        }
        return Triple(w, h, gray)
    }

    fun otsuBinary(name: String): BinaryImage {
        val (w, h, gray) = loadGray(name)
        return BinaryImage.fromGray(w, h, gray, otsuThreshold(gray))
    }

    /** Edge map via simple gradient magnitude, standing in for XDoG on photos. */
    fun edgeBinary(name: String, threshold: Int = 60): BinaryImage {
        val (w, h, g) = loadGray(name)
        val img = BinaryImage(w, h)
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val gx = g[y * w + x + 1] - g[y * w + x - 1]
            val gy = g[(y + 1) * w + x] - g[(y - 1) * w + x]
            img[x, y] = gx * gx + gy * gy > threshold * threshold
        }
        return img
    }

    private fun otsuThreshold(gray: IntArray): Int {
        val hist = IntArray(256).also { h -> gray.forEach { h[it]++ } }
        val total = gray.size
        val sumAll = hist.indices.sumOf { it.toLong() * hist[it] }
        var sumB = 0L
        var weightB = 0
        var best = 0.0
        var threshold = 128
        for (t in 0 until 256) {
            weightB += hist[t]
            if (weightB == 0) continue
            val weightF = total - weightB
            if (weightF == 0) break
            sumB += t.toLong() * hist[t]
            val meanB = sumB.toDouble() / weightB
            val meanF = (sumAll - sumB).toDouble() / weightF
            val between = weightB.toDouble() * weightF * (meanB - meanF) * (meanB - meanF)
            if (between > best) { best = between; threshold = t + 1 }
        }
        return threshold
    }
}
