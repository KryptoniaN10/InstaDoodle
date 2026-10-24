package com.instadoodle.engine

import kotlin.math.hypot

/** A 2D point. Engine output uses normalized coordinates in 0..1. */
data class Point(val x: Float, val y: Float) {
    fun distanceTo(other: Point): Float = hypot(x - other.x, y - other.y)
}

/** One continuous pen stroke (finger down -> move -> up). */
data class Stroke(val points: List<Point>) {
    val start: Point get() = points.first()
    val end: Point get() = points.last()

    /** Total polyline length in the stroke's own units. */
    val length: Float
        get() = points.zipWithNext().fold(0f) { acc, (a, b) -> acc + a.distanceTo(b) }

    fun reversed(): Stroke = Stroke(points.asReversed().toList())
}

/** Line extraction strategy. */
enum class SketchMode {
    /** Photos: Canny edge contours. */
    PHOTO,

    /** Logos / high-contrast art: Otsu threshold, filled regions become outlines. */
    LINE_ART,
}

/**
 * Tuning knobs for the image -> strokes pipeline.
 *
 * @param detail 0..1, higher keeps finer lines.
 * @param maxStrokes cap on output strokes; the longest are kept.
 * @param minStrokeLengthPx strokes shorter than this (in working-image pixels) are dropped.
 * @param mergeDistancePx strokes whose endpoints are closer than this are joined.
 * @param simplifyEpsilonPx Douglas-Peucker tolerance in pixels.
 */
data class EngineConfig(
    val mode: SketchMode = SketchMode.PHOTO,
    val detail: Float = 0.5f,
    val maxStrokes: Int = 250,
    val minStrokeLengthPx: Float = 8f,
    val mergeDistancePx: Float = 3f,
    val simplifyEpsilonPx: Float = 0.8f,
    val minComponentPx: Int = 6,
    val spurLengthPx: Int = 5,
    val smoothIterations: Int = 2,
    val orderStrokes: Boolean = true,
    val twoOptPasses: Int = 2,
) {
    companion object {
        const val MAX_WORKING_SIDE = 1024
    }
}

/** Sum of pen-up travel between consecutive strokes. */
fun travelDistance(strokes: List<Stroke>): Float =
    strokes.zipWithNext().fold(0f) { acc, (a, b) -> acc + a.end.distanceTo(b.start) }
