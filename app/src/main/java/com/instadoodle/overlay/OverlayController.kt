package com.instadoodle.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.instadoodle.data.SettingsStore
import com.instadoodle.data.SketchSession
import com.instadoodle.engine.DemoShapes
import com.instadoodle.engine.Stroke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns the overlay windows: a draggable control panel, a non-touchable placement
 * rectangle, and two small handles (move / resize) placed outside the rectangle so they
 * never intercept dispatched strokes. Handles are hidden while drawing.
 */
class OverlayController(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val player: GesturePlayer,
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private val handleSize = (44 * density).toInt()
    private val minRectSize = (80 * density).toInt()

    private var scope: CoroutineScope? = null
    private var progressJob: Job? = null
    private var showing = false

    private lateinit var controlView: LinearLayout
    private lateinit var progressText: TextView
    private lateinit var offsetText: TextView
    private lateinit var pauseButton: Button
    private val frameView = PlacementFrameView(context)
    private val moveHandle = DragHandleView(context, "✥", ::moveRect, ::persistRect)
    private val resizeHandle = DragHandleView(context, "⤡", ::resizeRect, ::persistRect)

    private val controlParams = overlayParams(touchable = true).apply { x = (16 * density).toInt(); y = (80 * density).toInt() }
    private val frameParams = overlayParams(touchable = false)
    private val moveParams = overlayParams(touchable = true).apply { width = handleSize; height = handleSize }
    private val resizeParams = overlayParams(touchable = true).apply { width = handleSize; height = handleSize }

    fun show() {
        if (showing) return
        showing = true
        controlView = buildControlPanel()
        applyRect(currentRect())
        wm.addView(frameView, frameParams)
        wm.addView(moveHandle, moveParams)
        wm.addView(resizeHandle, resizeParams)
        wm.addView(controlView, controlParams)
        scope = MainScope().also { s ->
            progressJob = s.launch { player.progress.collect(::renderProgress) }
            s.launch { settingsStore.settings.collect { offsetText.text = "offset ${it.offsetX}, ${it.offsetY}" } }
            s.launch { SketchSession.active.collect { if (player.progress.value.state != PlayerState.PLAYING) progressText.text = it.name } }
        }
    }

    fun hide() {
        if (!showing) return
        showing = false
        player.stop()
        scope?.cancel()
        scope = null
        listOf<View>(controlView, frameView, moveHandle, resizeHandle).forEach { runCatching { wm.removeView(it) } }
    }

    private fun startDrawing(strokes: List<Stroke>) {
        val s = settingsStore.settings.value
        val timing = GestureTiming(
            speedPxPerSec = s.speedPxPerSec.toFloat(),
            minStrokeMs = s.minStrokeMs.toLong(),
            maxStrokeMs = s.maxStrokeMs.toLong(),
            dotMs = s.dotMs.toLong(),
            maxGestureMs = DoodleAccessibilityService.maxGestureDurationMs(),
        )
        val planned = GestureMapper.map(strokes, currentRect(), s.offsetX.toFloat(), s.offsetY.toFloat(), timing)
        player.start(planned, s.interStrokeDelayMs.toLong())
    }

    private fun renderProgress(p: PlayerProgress) {
        val drawing = p.state == PlayerState.PLAYING || p.state == PlayerState.PAUSED
        val skipped = if (p.skipped > 0) " (${p.skipped} skipped)" else ""
        progressText.text = when (p.state) {
            PlayerState.IDLE -> SketchSession.active.value.name
            PlayerState.PLAYING -> "Drawing ${p.done}/${p.total}$skipped"
            PlayerState.PAUSED -> "Paused ${p.done}/${p.total}$skipped"
            PlayerState.FINISHED -> "Done ${p.done}/${p.total}$skipped"
            PlayerState.STOPPED -> "Stopped at ${p.done}/${p.total}"
        }
        pauseButton.text = if (p.state == PlayerState.PAUSED) "Resume" else "Pause"
        val handleVisibility = if (drawing) View.GONE else View.VISIBLE
        moveHandle.visibility = handleVisibility
        resizeHandle.visibility = handleVisibility
    }

    // region placement rectangle

    private fun screenSize(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun currentRect(): PlacementRect {
        val s = settingsStore.settings.value
        if (s.rectWidth > 0 && s.rectHeight > 0) {
            return PlacementRect(s.rectLeft.toFloat(), s.rectTop.toFloat(), s.rectWidth.toFloat(), s.rectHeight.toFloat())
        }
        val (w, h) = screenSize()
        val side = (w * 0.8f)
        return PlacementRect((w - side) / 2f, h * 0.3f, side, side)
    }

    private fun applyRect(rect: PlacementRect) {
        frameParams.x = rect.left.toInt()
        frameParams.y = rect.top.toInt()
        frameParams.width = rect.width.toInt()
        frameParams.height = rect.height.toInt()
        moveParams.x = frameParams.x - handleSize
        moveParams.y = frameParams.y - handleSize
        resizeParams.x = frameParams.x + frameParams.width
        resizeParams.y = frameParams.y + frameParams.height
        if (showing && frameView.isAttachedToWindow) {
            wm.updateViewLayout(frameView, frameParams)
            wm.updateViewLayout(moveHandle, moveParams)
            wm.updateViewLayout(resizeHandle, resizeParams)
        }
    }

    private var dragRect: PlacementRect? = null

    private fun moveRect(dx: Float, dy: Float) {
        val r = dragRect ?: currentRect()
        dragRect = r.copy(left = (r.left + dx).coerceAtLeast(0f), top = (r.top + dy).coerceAtLeast(0f)).also(::applyRect)
    }

    private fun resizeRect(dx: Float, dy: Float) {
        val r = dragRect ?: currentRect()
        dragRect = r.copy(
            width = (r.width + dx).coerceAtLeast(minRectSize.toFloat()),
            height = (r.height + dy).coerceAtLeast(minRectSize.toFloat()),
        ).also(::applyRect)
    }

    private fun persistRect() {
        val r = dragRect ?: return
        dragRect = null
        settingsStore.update {
            it.copy(rectLeft = r.left.toInt(), rectTop = r.top.toInt(), rectWidth = r.width.toInt(), rectHeight = r.height.toInt())
        }
    }

    // endregion

    // region control panel

    @SuppressLint("SetTextI18n")
    private fun buildControlPanel(): LinearLayout {
        val pad = (8 * density).toInt()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E6202124"))
                cornerRadius = 12 * density
            }
            addView(titleRow())
            progressText = label("").also { addView(it) }
            addView(row(
                button("Start") { startDrawing(SketchSession.active.value.strokes) },
                button("Pause") { if (player.progress.value.state == PlayerState.PAUSED) player.resume() else player.pause() }
                    .also { pauseButton = it },
                button("Stop") { player.stop() },
            ))
            addView(row(
                button("Calibrate") { startDrawing(DemoShapes.calibrationCross()) },
                button("Demo") { startDrawing(DemoShapes.demo()) },
            ))
            offsetText = label("").also { addView(it) }
            addView(row(
                button("X−") { nudge(-NUDGE_PX, 0) },
                button("X+") { nudge(NUDGE_PX, 0) },
                button("Y−") { nudge(0, -NUDGE_PX) },
                button("Y+") { nudge(0, NUDGE_PX) },
            ))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun titleRow(): View {
        val title = label("InstaDoodle  ✥ drag").apply { setTextColor(Color.parseColor("#F48FB1")) }
        var lastX = 0f
        var lastY = 0f
        title.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lastX = e.rawX; lastY = e.rawY }
                MotionEvent.ACTION_MOVE -> {
                    controlParams.x += (e.rawX - lastX).toInt()
                    controlParams.y += (e.rawY - lastY).toInt()
                    lastX = e.rawX
                    lastY = e.rawY
                    wm.updateViewLayout(controlView, controlParams)
                }
            }
            true
        }
        return row(title, button("✕") { hide() })
    }

    private fun nudge(dx: Int, dy: Int) = settingsStore.update { it.copy(offsetX = it.offsetX + dx, offsetY = it.offsetY + dy) }

    private fun label(text: String) = TextView(context).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 13f
        setPadding((4 * density).toInt(), (2 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
    }

    private fun button(text: String, onClick: () -> Unit) = Button(context).apply {
        this.text = text
        textSize = 12f
        isAllCaps = false
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
        setOnClickListener { onClick() }
    }

    private fun row(vararg views: View) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        views.forEach { addView(it) }
    }

    // endregion

    private fun overlayParams(touchable: Boolean) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private companion object {
        const val NUDGE_PX = 4
    }
}
