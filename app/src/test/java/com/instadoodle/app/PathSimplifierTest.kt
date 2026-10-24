package com.instadoodle.app

import com.instadoodle.app.drawing.PathSimplifier
import com.instadoodle.app.drawing.PointF
import com.instadoodle.app.drawing.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathSimplifierTest {

    @Test
    fun testRdpCollinearPointsSimplified() {
        // A straight horizontal line with multiple collinear points
        val points = listOf(
            PointF(0f, 0f),
            PointF(10f, 0f),
            PointF(20f, 0f),
            PointF(30f, 0f),
            PointF(40f, 0f)
        )
        val stroke = Stroke(points = points, closed = false)
        val simplified = PathSimplifier.simplify(stroke, epsilon = 1.0f)

        // All collinear intermediate points should be eliminated
        assertEquals(2, simplified.points.size)
        assertEquals(PointF(0f, 0f), simplified.points.first())
        assertEquals(PointF(40f, 0f), simplified.points.last())
    }

    @Test
    fun testRdpPreservesCorner() {
        // A triangle wave: (0,0) -> (10, 20) -> (20, 0)
        val points = listOf(
            PointF(0f, 0f),
            PointF(5f, 10f),
            PointF(10f, 20f),
            PointF(15f, 10f),
            PointF(20f, 0f)
        )
        val stroke = Stroke(points = points, closed = false)
        val simplified = PathSimplifier.simplify(stroke, epsilon = 1.0f)

        // Peak point at (10, 20) must be preserved because deviation is 20 > 1.0
        assertEquals(3, simplified.points.size)
        assertEquals(PointF(0f, 0f), simplified.points[0])
        assertEquals(PointF(10f, 20f), simplified.points[1])
        assertEquals(PointF(20f, 0f), simplified.points[2])
    }

    @Test
    fun testDiscardShortStrokes() {
        // Very short stroke with 4 points and tiny length
        val tinyStroke = Stroke(
            points = listOf(PointF(0f, 0f), PointF(1f, 1f), PointF(2f, 1f), PointF(2f, 2f))
        )
        // Valid long stroke
        val longPoints = (0..20).map { PointF(it.toFloat() * 5f, (it % 2).toFloat() * 10f) }
        val longStroke = Stroke(points = longPoints)

        val config = PathSimplifier.Config(
            epsilon = 1.0f,
            minPoints = 8,
            minArcLength = 15.0f,
            optimizeOrder = false
        )

        val processed = PathSimplifier.process(listOf(tinyStroke, longStroke), config)
        assertEquals(1, processed.size)
        assertTrue(processed.first().calculateArcLength() > 15.0f)
    }

    @Test
    fun testGreedyNearestNeighborReversesWhenEndIsCloser() {
        // Stroke 1: from (0, 0) to (10, 0)
        val stroke1 = Stroke(points = listOf(PointF(0f, 0f), PointF(10f, 0f)))

        // Stroke 2: defined from (100, 0) to (12, 0)
        // Stroke 2's end (12, 0) is only 2px from stroke 1's end (10, 0),
        // whereas Stroke 2's start (100, 0) is 90px away!
        val stroke2 = Stroke(points = listOf(PointF(100f, 0f), PointF(50f, 0f), PointF(12f, 0f)))

        val ordered = PathSimplifier.orderStrokesGreedy(
            strokes = listOf(stroke2, stroke1),
            initialPos = PointF(0f, 0f)
        )

        assertEquals(2, ordered.size)
        assertEquals(stroke1, ordered[0])

        // stroke2 must have been chosen next, and REVERSED so it starts at (12, 0) and ends at (100, 0)
        val chosenStroke2 = ordered[1]
        assertEquals(PointF(12f, 0f), chosenStroke2.startPoint)
        assertEquals(PointF(100f, 0f), chosenStroke2.endPoint)
    }

    @Test
    fun testPerpendicularDistanceCalculation() {
        val p = PointF(5f, 10f)
        val a = PointF(0f, 0f)
        val b = PointF(10f, 0f)

        val dist = PathSimplifier.perpendicularDistance(p, a, b)
        assertEquals(10f, dist, 0.001f)
    }
}
