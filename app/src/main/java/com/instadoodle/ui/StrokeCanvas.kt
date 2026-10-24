package com.instadoodle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import com.instadoodle.engine.Stroke

/** Draws normalized strokes; [progress] 0..1 reveals strokes progressively for the replay animation. */
@Composable
fun StrokeCanvas(strokes: List<Stroke>, modifier: Modifier = Modifier, progress: Float = 1f, strokeWidth: Float = 3f) {
    Canvas(modifier.aspectRatio(1f).background(Color.White)) {
        val side = size.minDimension
        val visible = (strokes.size * progress.coerceIn(0f, 1f)).toInt()
        val style = DrawStroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        strokes.take(visible).forEach { stroke ->
            if (stroke.points.size == 1) {
                drawCircle(Color.Black, strokeWidth, Offset(stroke.start.x * side, stroke.start.y * side))
            } else {
                val path = Path().apply {
                    moveTo(stroke.start.x * side, stroke.start.y * side)
                    stroke.points.drop(1).forEach { lineTo(it.x * side, it.y * side) }
                }
                drawPath(path, Color.Black, style = style)
            }
        }
    }
}
