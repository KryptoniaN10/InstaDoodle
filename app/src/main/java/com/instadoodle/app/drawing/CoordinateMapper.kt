package com.instadoodle.app.drawing

import kotlin.math.min

/**
 * Maps sketch coordinates into a target screen rectangle while preserving aspect ratio and centering the sketch.
 *
 * @property sketchWidth Original width of the sketch in pixels.
 * @property sketchHeight Original height of the sketch in pixels.
 * @property targetLeft Left coordinate of the target destination rectangle.
 * @property targetTop Top coordinate of the target destination rectangle.
 * @property targetWidth Width of the target destination rectangle.
 * @property targetHeight Height of the target destination rectangle.
 */
class CoordinateMapper(
    val sketchWidth: Float,
    val sketchHeight: Float,
    val targetLeft: Float,
    val targetTop: Float,
    val targetWidth: Float,
    val targetHeight: Float
) {
    val scale: Float
    val offsetX: Float
    val offsetY: Float
    val destinationWidth: Float
    val destinationHeight: Float

    init {
        require(sketchWidth > 0 && sketchHeight > 0) { "Sketch dimensions must be positive" }
        require(targetWidth > 0 && targetHeight > 0) { "Target bounds must be positive" }

        val scaleX = targetWidth / sketchWidth
        val scaleY = targetHeight / sketchHeight
        scale = min(scaleX, scaleY)

        destinationWidth = sketchWidth * scale
        destinationHeight = sketchHeight * scale

        // Center content inside target bounding box
        offsetX = targetLeft + (targetWidth - destinationWidth) / 2f
        offsetY = targetTop + (targetHeight - destinationHeight) / 2f
    }

    /**
     * Maps a sketch coordinate to destination coordinates.
     */
    fun mapPoint(point: PointF): PointF {
        return PointF(
            x = point.x * scale + offsetX,
            y = point.y * scale + offsetY
        )
    }

    /**
     * Maps a stroke's points into destination coordinates.
     */
    fun mapStroke(stroke: Stroke): Stroke {
        return Stroke(
            points = stroke.points.map { mapPoint(it) },
            closed = stroke.closed
        )
    }

    /**
     * Maps all strokes in a sketch into destination coordinates.
     */
    fun mapSketch(sketch: Sketch): List<Stroke> {
        return sketch.strokes.map { mapStroke(it) }
    }

    /**
     * Inverts a destination screen point back to sketch coordinates.
     */
    fun unmapPoint(targetPoint: PointF): PointF {
        return PointF(
            x = (targetPoint.x - offsetX) / scale,
            y = (targetPoint.y - offsetY) / scale
        )
    }
}
