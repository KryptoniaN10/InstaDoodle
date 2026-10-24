package com.instadoodle.app.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/**
 * Provides built-in sample bitmaps for fast debugging and testing without requiring file picker access.
 */
object SampleImages {

    fun createSampleStar(size: Int = 400): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        val centerX = size / 2f
        val centerY = size / 2f
        val outerRadius = size * 0.4f
        val innerRadius = size * 0.18f
        val numPoints = 5

        val path = Path()
        for (i in 0 until numPoints * 2) {
            val angle = (i * Math.PI / numPoints) - (Math.PI / 2)
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = (centerX + r * cos(angle)).toFloat()
            val y = (centerY + r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)

        // Draw an inner decorative circle
        canvas.drawCircle(centerX, centerY, innerRadius * 0.8f, paint)

        return bitmap
    }

    fun createSampleLogo(size: Int = 400): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

        val cx = size / 2f
        val cy = size / 2f

        // Draw interlocking circles
        canvas.drawCircle(cx - 50f, cy, 90f, paint)
        canvas.drawCircle(cx + 50f, cy, 90f, paint)
        canvas.drawCircle(cx, cy - 40f, 60f, paint)

        return bitmap
    }
}
