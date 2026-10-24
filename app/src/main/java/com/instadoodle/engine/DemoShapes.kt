package com.instadoodle.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Hardcoded normalized shapes used to verify gesture dispatch without an image. */
object DemoShapes {

    fun spiral(turns: Int = 4, pointsPerTurn: Int = 48): Stroke {
        val total = turns * pointsPerTurn
        return Stroke((0..total).map { i ->
            val t = i.toFloat() / total
            val angle = t * turns * 2 * PI
            val r = 0.05f + 0.4f * t
            Point(0.5f + r * cos(angle).toFloat(), 0.5f + r * sin(angle).toFloat())
        })
    }

    fun square(inset: Float = 0.1f): Stroke = Stroke(
        listOf(
            Point(inset, inset), Point(1 - inset, inset), Point(1 - inset, 1 - inset),
            Point(inset, 1 - inset), Point(inset, inset),
        ),
    )

    /** Spiral + square, drawn as two strokes. */
    fun demo(): List<Stroke> = listOf(square(), spiral())

    /** Calibration cross with a centre dot. */
    fun calibrationCross(): List<Stroke> = listOf(
        Stroke(listOf(Point(0f, 0.5f), Point(1f, 0.5f))),
        Stroke(listOf(Point(0.5f, 0f), Point(0.5f, 1f))),
        Stroke(listOf(Point(0.5f, 0.5f))),
    )
}
