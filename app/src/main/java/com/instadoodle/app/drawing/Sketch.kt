package com.instadoodle.app.drawing

import kotlinx.serialization.Serializable
import kotlin.math.hypot

/**
 * Represents a 2D floating-point coordinate within a sketch coordinate system.
 */
@Serializable
data class PointF(val x: Float, val y: Float) {
    /**
     * Calculates the Euclidean distance to another point.
     */
    fun distanceTo(other: PointF): Float = hypot(x - other.x, y - other.y)

    /**
     * Returns true if the coordinates are finite and non-NaN.
     */
    val isValid: Boolean get() = !x.isNaN() && !y.isNaN() && !x.isInfinite() && !y.isInfinite()
}

/**
 * Represents a single stroke composed of sequential points.
 *
 * @property points Ordered list of points defining the path.
 * @property closed True if the stroke forms a closed polygon (end connects to start).
 */
@Serializable
data class Stroke(
    val points: List<PointF>,
    val closed: Boolean = false
) {
    val pointCount: Int get() = points.size

    val startPoint: PointF? get() = points.firstOrNull()
    val endPoint: PointF? get() = points.lastOrNull()

    /**
     * Total length of the stroke along its path.
     */
    fun calculateArcLength(): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 0 until points.size - 1) {
            total += points[i].distanceTo(points[i + 1])
        }
        if (closed && points.size > 2) {
            total += points.last().distanceTo(points.first())
        }
        return total
    }

    /**
     * Returns a reversed stroke.
     */
    fun reversed(): Stroke = Stroke(points = points.reversed(), closed = closed)
}

/**
 * Represents a complete vector sketch containing dimension metadata and stroke paths.
 *
 * @property width Native width in pixels.
 * @property height Native height in pixels.
 * @property strokes Ordered list of drawing strokes.
 */
@Serializable
data class Sketch(
    val width: Int,
    val height: Int,
    val strokes: List<Stroke>
) {
    val totalPointCount: Int get() = strokes.sumOf { it.pointCount }
    val strokeCount: Int get() = strokes.size

    val isEmpty: Boolean get() = strokes.isEmpty() || totalPointCount == 0

    companion object {
        val EMPTY = Sketch(0, 0, emptyList())
    }
}
