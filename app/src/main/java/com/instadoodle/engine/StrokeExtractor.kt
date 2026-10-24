package com.instadoodle.engine

/**
 * Pure-Kotlin half of the pipeline: binary line image -> thinned skeleton -> traced,
 * simplified, filtered, ordered strokes normalized to 0..1.
 */
object StrokeExtractor {

    fun extract(binary: BinaryImage, config: EngineConfig): List<Stroke> {
        val skeleton = Thinning.thin(binary)
        val traced = SkeletonTracer.trace(skeleton)
        val merged = StrokeFilter.mergeClose(traced, config.mergeDistancePx)
        val simplified = merged.map { Simplifier.simplify(it, config.simplifyEpsilonPx) }
        val kept = StrokeFilter.capLongest(StrokeFilter.dropShort(simplified, config.minStrokeLengthPx), config.maxStrokes)
        val normalized = normalize(kept, binary.width, binary.height)
        return if (config.orderStrokes) StrokeOrderer.order(normalized, config.twoOptPasses) else normalized
    }

    /** Scales pixel coordinates into 0..1 using the longer side so aspect ratio is preserved. */
    fun normalize(strokes: List<Stroke>, width: Int, height: Int): List<Stroke> {
        val scale = (maxOf(width, height) - 1).coerceAtLeast(1).toFloat()
        val offsetX = (scale - (width - 1)) / 2f
        val offsetY = (scale - (height - 1)) / 2f
        return strokes.map { s ->
            Stroke(s.points.map { p ->
                Point(((p.x + offsetX) / scale).coerceIn(0f, 1f), ((p.y + offsetY) / scale).coerceIn(0f, 1f))
            })
        }
    }
}
