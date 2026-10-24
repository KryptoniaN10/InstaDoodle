package com.instadoodle.overlay

import com.instadoodle.engine.Point
import com.instadoodle.engine.Stroke

/** Placement rectangle in screen pixels. */
data class PlacementRect(val left: Float, val top: Float, val width: Float, val height: Float)

/** Timing rules for turning a stroke into a gesture. */
data class GestureTiming(
    val speedPxPerSec: Float,
    val minStrokeMs: Long,
    val maxStrokeMs: Long,
    val dotMs: Long,
    /** AccessibilityService.GestureDescription.getMaxGestureDuration() on the device. */
    val maxGestureMs: Long,
)

/** A stroke in screen pixels plus how long the finger takes to draw it. */
data class PlannedStroke(val points: List<Point>, val durationMs: Long) {
    val isDot: Boolean get() = points.size == 1
}

/** Maps normalized strokes onto the placement rectangle. Pure Kotlin for unit testing. */
object GestureMapper {

    /**
     * The engine normalizes into a unit square, so strokes are fitted into the largest
     * centred square of [rect] to keep the aspect ratio, then shifted by the calibration offset.
     */
    fun map(strokes: List<Stroke>, rect: PlacementRect, offsetX: Float, offsetY: Float, timing: GestureTiming): List<PlannedStroke> {
        val side = minOf(rect.width, rect.height)
        val originX = rect.left + (rect.width - side) / 2f + offsetX
        val originY = rect.top + (rect.height - side) / 2f + offsetY
        return strokes.filter { it.points.isNotEmpty() }.map { stroke ->
            val pts = stroke.points.map { p ->
                // Gesture paths must not have negative coordinates.
                Point((originX + p.x * side).coerceAtLeast(0f), (originY + p.y * side).coerceAtLeast(0f))
            }
            val screenStroke = Stroke(pts)
            if (screenStroke.length < 1f) {
                PlannedStroke(listOf(pts.first()), timing.dotMs.coerceAtLeast(1))
            } else {
                PlannedStroke(pts, durationFor(screenStroke.length, timing))
            }
        }
    }

    /** Duration proportional to length / speed, clamped to settings and the platform limit. */
    fun durationFor(lengthPx: Float, timing: GestureTiming): Long {
        val upper = minOf(timing.maxStrokeMs, timing.maxGestureMs).coerceAtLeast(1)
        val lower = timing.minStrokeMs.coerceIn(1, upper)
        val raw = (lengthPx / timing.speedPxPerSec.coerceAtLeast(1f) * 1000f).toLong()
        return raw.coerceIn(lower, upper)
    }
}
