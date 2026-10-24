package com.instadoodle.app

import com.instadoodle.app.drawing.CoordinateMapper
import com.instadoodle.app.drawing.PointF
import com.instadoodle.app.drawing.Stroke
import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateMapperTest {

    @Test
    fun testUniformScalePreservesAspectRatio() {
        // Sketch: 200 x 100 (2:1 aspect ratio)
        // Target: 400 x 400
        val mapper = CoordinateMapper(
            sketchWidth = 200f,
            sketchHeight = 100f,
            targetLeft = 0f,
            targetTop = 0f,
            targetWidth = 400f,
            targetHeight = 400f
        )

        // Scale should be limited by width: 400 / 200 = 2.0
        // Scaled height: 100 * 2.0 = 200
        // Centering in height: (400 - 200) / 2 = 100 offsetY
        assertEquals(2.0f, mapper.scale, 0.001f)
        assertEquals(0.0f, mapper.offsetX, 0.001f)
        assertEquals(100.0f, mapper.offsetY, 0.001f)
    }

    @Test
    fun testPointMappingAndCentering() {
        // Sketch: 100 x 100
        // Target: 200 x 200 at offset (50, 50)
        val mapper = CoordinateMapper(
            sketchWidth = 100f,
            sketchHeight = 100f,
            targetLeft = 50f,
            targetTop = 50f,
            targetWidth = 200f,
            targetHeight = 200f
        )

        // Center of sketch (50, 50) should map to center of target (150, 150)
        val centerMapped = mapper.mapPoint(PointF(50f, 50f))
        assertEquals(150f, centerMapped.x, 0.001f)
        assertEquals(150f, centerMapped.y, 0.001f)
    }

    @Test
    fun testUnmapPointRoundtrip() {
        val mapper = CoordinateMapper(
            sketchWidth = 320f,
            sketchHeight = 480f,
            targetLeft = 100f,
            targetTop = 200f,
            targetWidth = 600f,
            targetHeight = 800f
        )

        val original = PointF(123.4f, 234.5f)
        val mapped = mapper.mapPoint(original)
        val unmapped = mapper.unmapPoint(mapped)

        assertEquals(original.x, unmapped.x, 0.01f)
        assertEquals(original.y, unmapped.y, 0.01f)
    }

    @Test
    fun testStrokeMapping() {
        val mapper = CoordinateMapper(
            sketchWidth = 100f,
            sketchHeight = 100f,
            targetLeft = 0f,
            targetTop = 0f,
            targetWidth = 200f,
            targetHeight = 200f
        )

        val stroke = Stroke(
            points = listOf(PointF(0f, 0f), PointF(100f, 100f)),
            closed = true
        )

        val mappedStroke = mapper.mapStroke(stroke)
        assertEquals(2, mappedStroke.points.size)
        assertTrue(mappedStroke.closed)
        assertEquals(PointF(0f, 0f), mappedStroke.points[0])
        assertEquals(PointF(200f, 200f), mappedStroke.points[1])
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
