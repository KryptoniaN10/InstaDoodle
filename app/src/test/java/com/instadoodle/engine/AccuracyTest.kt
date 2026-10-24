package com.instadoodle.engine

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Measures how faithfully strokes reproduce the cleaned input's centre lines
 * (speckles and spurs removed, as the extractor does):
 * recall = share of skeleton pixels within [TOL] px of a stroke,
 * precision = share of stroke samples within [TOL] px of a skeleton pixel.
 */
class AccuracyTest {

    private val samples = mapOf(
        "logo.png" to { TestImages.otsuBinary("logo.png") },
        "handwriting.png" to { TestImages.otsuBinary("handwriting.png") },
        "portrait.jpg" to { TestImages.edgeBinary("portrait.jpg") },
    )

    @Test
    fun strokesStayCloseToSourceLines() {
        for ((name, load) in samples) {
            val binary = load()
            val strokes = StrokeExtractor.extract(binary, EngineConfig(maxStrokes = 250))
            val (recall, precision) = score(binary, strokes)
            println("ACCURACY $name strokes=${strokes.size} recall=%.3f precision=%.3f".format(recall, precision))
            assertTrue("$name recall $recall", recall >= 0.85f)
            assertTrue("$name precision $precision", precision >= 0.95f)
        }
    }

    private fun score(binary: BinaryImage, strokes: List<Stroke>): Pair<Float, Float> {
        val config = EngineConfig()
        val cleaned = Cleanup.removeSmallComponents(binary, config.minComponentPx)
        val skeleton = Cleanup.pruneSpurs(Thinning.thin(cleaned), config.spurLengthPx)
        val w = binary.width
        val h = binary.height
        val scale = (maxOf(w, h) - 1).toFloat()
        val ox = (scale - (w - 1)) / 2f
        val oy = (scale - (h - 1)) / 2f
        val samplesPx = strokes.flatMap { s ->
            val pts = s.points.map { Point(it.x * scale - ox, it.y * scale - oy) }
            if (pts.size == 1) pts else pts.zipWithNext().flatMap { (a, b) ->
                val n = ceil(a.distanceTo(b) * 2).toInt().coerceAtLeast(1)
                (0..n).map { i -> Point(a.x + (b.x - a.x) * i / n, a.y + (b.y - a.y) * i / n) }
            }
        }
        val near = BooleanArray(w * h)
        val r = ceil(TOL).toInt()
        for (p in samplesPx) {
            val cx = p.x.roundToInt()
            val cy = p.y.roundToInt()
            for (dy in -r..r) for (dx in -r..r) {
                val x = cx + dx
                val y = cy + dy
                if (x in 0 until w && y in 0 until h && hypot(dx.toFloat(), dy.toFloat()) <= TOL) near[y * w + x] = true
            }
        }
        var skel = 0
        var hit = 0
        for (y in 0 until h) for (x in 0 until w) if (skeleton[x, y]) {
            skel++
            if (near[y * w + x]) hit++
        }
        val inkNear = BooleanArray(w * h)
        for (y in 0 until h) for (x in 0 until w) if (skeleton[x, y]) {
            for (dy in -r..r) for (dx in -r..r) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until w && ny in 0 until h && hypot(dx.toFloat(), dy.toFloat()) <= TOL) inkNear[ny * w + nx] = true
            }
        }
        val good = samplesPx.count { p ->
            val x = p.x.roundToInt()
            val y = p.y.roundToInt()
            x in 0 until w && y in 0 until h && inkNear[y * w + x]
        }
        return hit.toFloat() / skel.coerceAtLeast(1) to good.toFloat() / samplesPx.size.coerceAtLeast(1)
    }

    private companion object {
        const val TOL = 2f
    }
}
