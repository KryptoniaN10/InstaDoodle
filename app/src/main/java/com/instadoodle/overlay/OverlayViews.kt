package com.instadoodle.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

/** Dashed placement rectangle. Its window is FLAG_NOT_TOUCHABLE so gestures pass through. */
class PlacementFrameView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = Color.parseColor("#E91E63")
        pathEffect = DashPathEffect(floatArrayOf(10f * density, 6f * density), 0f)
    }
    private val fill = Paint().apply { color = Color.argb(18, 233, 30, 99) }

    override fun onDraw(canvas: Canvas) {
        val inset = border.strokeWidth / 2
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        canvas.drawRect(inset, inset, width - inset, height - inset, border)
    }
}

/** Round drag handle that reports raw pointer deltas. */
@SuppressLint("ViewConstructor")
class DragHandleView(
    context: Context,
    private val label: String,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
    private val onDragEnd: () -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CCE91E63") }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 16f * density
        textAlign = Paint.Align.CENTER
    }
    private var lastX = 0f
    private var lastY = 0f

    override fun onDraw(canvas: Canvas) {
        val r = minOf(width, height) / 2f
        canvas.drawCircle(width / 2f, height / 2f, r, circle)
        canvas.drawText(label, width / 2f, height / 2f - (text.ascent() + text.descent()) / 2, text)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY }
            MotionEvent.ACTION_MOVE -> {
                onDrag(event.rawX - lastX, event.rawY - lastY)
                lastX = event.rawX
                lastY = event.rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onDragEnd()
        }
        return true
    }
}
