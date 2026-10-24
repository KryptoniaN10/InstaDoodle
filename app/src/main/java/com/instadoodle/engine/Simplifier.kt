package com.instadoodle.engine

import kotlin.math.abs
import kotlin.math.hypot

/** Ramer-Douglas-Peucker polyline simplification (equivalent to OpenCV approxPolyDP). */
object Simplifier {

    fun simplify(stroke: Stroke, epsilon: Float): Stroke {
        if (stroke.points.size < 3) return stroke
        val keep = BooleanArray(stroke.points.size)
        keep[0] = true
        keep[keep.lastIndex] = true
        simplifyRange(stroke.points, 0, stroke.points.lastIndex, epsilon, keep)
        return Stroke(stroke.points.filterIndexed { i, _ -> keep[i] })
    }

    private fun simplifyRange(pts: List<Point>, first: Int, last: Int, epsilon: Float, keep: BooleanArray) {
        if (last <= first + 1) return
        var maxDist = -1f
        var index = first
        for (i in first + 1 until last) {
            val d = perpendicularDistance(pts[i], pts[first], pts[last])
            if (d > maxDist) {
                maxDist = d
                index = i
            }
        }
        if (maxDist > epsilon) {
            keep[index] = true
            simplifyRange(pts, first, index, epsilon, keep)
            simplifyRange(pts, index, last, epsilon, keep)
        }
    }

    private fun perpendicularDistance(p: Point, a: Point, b: Point): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy)
        if (len == 0f) return p.distanceTo(a)
        return abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x) / len
    }
}
