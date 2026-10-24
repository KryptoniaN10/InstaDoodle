package com.instadoodle.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineUnitTest {

    @Test
    fun thinningReducesThickBarToSingleLine() {
        val img = BinaryImage(40, 20)
        for (y in 5..11) for (x in 5..34) img[x, y] = true
        val thin = Thinning.thin(img)
        for (x in 10..29) {
            val column = (0 until 20).count { thin[x, it] }
            assertEquals("column $x should be 1px wide", 1, column)
        }
    }

    @Test
    fun tracerFollowsOpenLineAsOneStroke() {
        val img = BinaryImage(30, 30)
        for (i in 2..27) img[i, i] = true
        val strokes = SkeletonTracer.trace(img)
        assertEquals(1, strokes.size)
        assertEquals(26, strokes.first().points.size)
    }

    @Test
    fun simplifierCollapsesStraightLine() {
        val line = Stroke((0..50).map { Point(it.toFloat(), it * 2f) })
        assertEquals(2, Simplifier.simplify(line, 0.5f).points.size)
    }

    @Test
    fun mergeJoinsCloseEndpointsWithReversal() {
        val a = Stroke(listOf(Point(0f, 0f), Point(10f, 0f)))
        val b = Stroke(listOf(Point(20f, 0f), Point(11f, 0f)))
        val merged = StrokeFilter.mergeClose(listOf(a, b), 2f)
        assertEquals(1, merged.size)
        assertEquals(20f, merged.first().length, 0.01f)
    }

    @Test
    fun capKeepsLongestStrokes() {
        val strokes = (1..10).map { Stroke(listOf(Point(0f, 0f), Point(it.toFloat(), 0f))) }
        val kept = StrokeFilter.capLongest(strokes, 3)
        assertEquals(listOf(10f, 9f, 8f), kept.map { it.length })
    }

    @Test
    fun ordererNeverWorseAndReversesStrokes() {
        val strokes = listOf(
            Stroke(listOf(Point(0.9f, 0f), Point(1f, 0f))),
            Stroke(listOf(Point(0.1f, 0f), Point(0f, 0f))),
            Stroke(listOf(Point(0.5f, 0f), Point(0.6f, 0f))),
        )
        val ordered = StrokeOrderer.order(strokes)
        assertTrue(travelDistance(ordered) < travelDistance(strokes))
        assertEquals(Point(0f, 0f), ordered.first().start)
    }

    @Test
    fun normalizePreservesAspectRatioInUnitSquare() {
        val s = StrokeExtractor.normalize(listOf(Stroke(listOf(Point(0f, 0f), Point(99f, 49f)))), 100, 50)
        val pts = s.first().points
        assertEquals(0f, pts[0].x, 1e-4f)
        assertEquals(25f / 99f, pts[0].y, 1e-4f)
        assertEquals(1f, pts[1].x, 1e-4f)
        assertEquals(0.75f, pts[1].y, 0.01f)
    }

    @Test
    fun emptyImageGivesNoStrokes() {
        assertTrue(StrokeExtractor.extract(BinaryImage(10, 10), EngineConfig()).isEmpty())
    }
}
