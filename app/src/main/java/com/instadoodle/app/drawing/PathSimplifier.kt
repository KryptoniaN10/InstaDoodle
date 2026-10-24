package com.instadoodle.app.drawing

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Handles Ramer-Douglas-Peucker curve simplification, short stroke filtering,
 * and greedy nearest-neighbor stroke path ordering.
 */
object PathSimplifier {

    data class Config(
        val epsilon: Float = 1.5f,
        val minPoints: Int = 8,
        val minArcLength: Float = 6.0f,
        val optimizeOrder: Boolean = true
    )

    /**
     * Simplifies, filters, and orders a list of strokes according to [config].
     *
     * @param strokes Raw input strokes.
     * @param config Simplification and ordering parameters.
     * @return Cleaned, optimized list of strokes.
     */
    fun process(strokes: List<Stroke>, config: Config): List<Stroke> {
        if (strokes.isEmpty()) return emptyList()

        // Step 1: Simplify each stroke with Douglas-Peucker
        val simplified = strokes.map { stroke ->
            simplify(stroke, config.epsilon)
        }

        // Step 2: Discard short strokes
        val filtered = simplified.filter { stroke ->
            stroke.points.size >= config.minPoints &&
                    stroke.calculateArcLength() >= config.minArcLength
        }

        // Step 3: Order strokes with greedy nearest-neighbor
        return if (config.optimizeOrder) {
            orderStrokesGreedy(filtered)
        } else {
            filtered
        }
    }

    /**
     * Simplifies a single stroke using the Ramer-Douglas-Peucker algorithm.
     */
    fun simplify(stroke: Stroke, epsilon: Float): Stroke {
        if (stroke.points.size <= 2 || epsilon <= 0f) return stroke

        val simplifiedPoints = if (stroke.closed && stroke.points.size > 3) {
            // For closed loops, preserve closure by anchoring at midpoint
            val mid = stroke.points.size / 2
            val part1 = rdp(stroke.points.subList(0, mid + 1), epsilon)
            val part2 = rdp(stroke.points.subList(mid, stroke.points.size), epsilon)
            (part1.dropLast(1) + part2)
        } else {
            rdp(stroke.points, epsilon)
        }

        return Stroke(points = simplifiedPoints, closed = stroke.closed)
    }

    private fun rdp(points: List<PointF>, epsilon: Float): List<PointF> {
        if (points.size <= 2) return points

        val first = points.first()
        val last = points.last()

        var maxDistance = 0f
        var index = 0

        for (i in 1 until points.size - 1) {
            val d = perpendicularDistance(points[i], first, last)
            if (d > maxDistance) {
                maxDistance = d
                index = i
            }
        }

        return if (maxDistance > epsilon) {
            val left = rdp(points.subList(0, index + 1), epsilon)
            val right = rdp(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    /**
     * Computes perpendicular distance from point p to line segment (a, b).
     */
    internal fun perpendicularDistance(p: PointF, a: PointF, b: PointF): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lineLenSq = dx * dx + dy * dy

        if (lineLenSq == 0f) {
            return p.distanceTo(a)
        }

        // Distance formula: |(b.y - a.y)*p.x - (b.x - a.x)*p.y + b.x*a.y - b.y*a.x| / lineLen
        val numerator = abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x)
        return numerator / hypot(dx, dy)
    }

    /**
     * Orders strokes greedily using nearest-neighbor heuristic to minimize pen travel distance.
     * If the end point of a candidate stroke is closer than its start point, the stroke is reversed.
     */
    fun orderStrokesGreedy(strokes: List<Stroke>, initialPos: PointF = PointF(0f, 0f)): List<Stroke> {
        if (strokes.size <= 1) return strokes

        val remaining = strokes.toMutableList()
        val ordered = ArrayList<Stroke>(strokes.size)
        var currentPos = initialPos

        while (remaining.isNotEmpty()) {
            var bestIndex = -1
            var bestDistance = Float.MAX_VALUE
            var reverseBest = false

            for (i in remaining.indices) {
                val candidate = remaining[i]
                val start = candidate.startPoint ?: continue
                val end = candidate.endPoint ?: continue

                val distToStart = currentPos.distanceTo(start)
                val distToEnd = currentPos.distanceTo(end)

                if (distToStart < bestDistance) {
                    bestDistance = distToStart
                    bestIndex = i
                    reverseBest = false
                }
                // For open strokes, check if reversing saves travel distance
                if (!candidate.closed && distToEnd < bestDistance) {
                    bestDistance = distToEnd
                    bestIndex = i
                    reverseBest = true
                }
            }

            if (bestIndex == -1) {
                // Fallback in case of degenerate strokes
                ordered.addAll(remaining)
                break
            }

            val chosen = remaining.removeAt(bestIndex)
            val strokeToAdd = if (reverseBest) chosen.reversed() else chosen
            ordered.add(strokeToAdd)
            currentPos = strokeToAdd.endPoint ?: currentPos
        }

        return ordered
    }
}
