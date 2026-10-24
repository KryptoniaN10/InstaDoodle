package com.instadoodle.engine

import org.junit.Assert.assertTrue
import org.junit.Test

class SampleImagesTest {

    private val samples = mapOf(
        "logo.png" to { TestImages.otsuBinary("logo.png") },
        "handwriting.png" to { TestImages.otsuBinary("handwriting.png") },
        "portrait.jpg" to { TestImages.edgeBinary("portrait.jpg") },
    )

    @Test
    fun strokeCountRespectsCap() {
        for ((name, load) in samples) {
            for (cap in listOf(250, 40)) {
                val strokes = StrokeExtractor.extract(load(), EngineConfig(maxStrokes = cap))
                assertTrue("$name produced no strokes", strokes.isNotEmpty())
                assertTrue("$name: ${strokes.size} > $cap", strokes.size <= cap)
            }
        }
    }

    @Test
    fun allPointsAreNormalized() {
        for ((name, load) in samples) {
            val strokes = StrokeExtractor.extract(load(), EngineConfig())
            strokes.flatMap { it.points }.forEach { p ->
                assertTrue("$name point out of range: $p", p.x in 0f..1f && p.y in 0f..1f)
            }
        }
    }

    @Test
    fun orderingReducesTravel() {
        for ((name, load) in samples) {
            val unordered = StrokeExtractor.extract(load(), EngineConfig(orderStrokes = false))
            val ordered = StrokeOrderer.order(unordered)
            val before = travelDistance(unordered)
            val after = travelDistance(ordered)
            println("$name: ${unordered.size} strokes, travel $before -> $after")
            assertTrue("$name: travel $after !< $before", after < before)
        }
    }
}
